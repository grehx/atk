/*
// Copyright (c) 2015 Intel Corporation 
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/

package org.trustedanalytics.atk.giraph.plugins.model.lda

import org.apache.spark.frame.FrameRdd
import org.apache.spark.sql.parquet.atk.giraph.frame.lda.{ LdaParquetFrameVertexOutputFormat, LdaParquetFrameEdgeInputFormat }
import org.trustedanalytics.atk.engine.EngineConfig
import org.trustedanalytics.atk.engine.frame.SparkFrame
import org.trustedanalytics.atk.engine.model.Model
import org.trustedanalytics.atk.giraph.algorithms.lda.CVB0LDAComputation
import org.trustedanalytics.atk.giraph.algorithms.lda.CVB0LDAComputation.{ CVB0LDAAggregatorWriter, CVB0LDAMasterCompute }
import org.trustedanalytics.atk.giraph.config.lda._
import org.trustedanalytics.atk.giraph.plugins.util.{ GiraphConfigurationUtil, GiraphJobManager }
import org.trustedanalytics.atk.domain.CreateEntityArgs
import org.trustedanalytics.atk.domain.schema.{ DataTypes, Column, FrameSchema }
import org.trustedanalytics.atk.engine.plugin._
import spray.json._
import LdaJsonFormat._

/**
 * Latent Dirichlet allocation
 */
@PluginDoc(oneLine = "Creates Latent Dirichlet Allocation model",
  extended = """See the discussion about `Latent Dirichlet Allocation at Wikipedia. <http://en.wikipedia.org/wiki/Latent_Dirichlet_allocation>`__""",
  returns = """dict
    The data returned is composed of multiple components:
topic_given_doc : Frame
    Frame with conditional probabilities of topic given document.
word_given_topic : Frame
    Frame with conditional probabilities of word given topic.
topic_given_word : Frame
    Frame with conditional probabilities of topic given word.
report : str
   The configuration and learning curve report for Latent Dirichlet
   Allocation as a multiple line str.""")
class LdaTrainPlugin
    extends SparkCommandPlugin[LdaTrainArgs, LdaTrainResult] {

  /**
   * The name of the command, e.g. graphs/ml/loopy_belief_propagation
   *
   * The format of the name determines how the plugin gets "installed" in the client layer
   * e.g Python client via code generation.
   */
  override def name: String = "model:lda/train"

  override def apiMaturityTag = Some(ApiMaturityTag.Beta)

  override def execute(arguments: LdaTrainArgs)(implicit invocation: Invocation): LdaTrainResult = {

    val frames = engine.frames

    // validate arguments
    val frame = frames.expectFrame(arguments.frame)
    frame.schema.requireColumnIsType(arguments.documentColumnName, DataTypes.string)
    frame.schema.requireColumnIsType(arguments.wordColumnName, DataTypes.string)
    frame.schema.requireColumnIsType(arguments.wordCountColumnName, DataTypes.int64)
    require(frame.isParquet, "frame must be stored as parquet file, or support for new input format is needed")

    // setup and run
    val hConf = GiraphConfigurationUtil.newHadoopConfigurationFrom(EngineConfig.config, "trustedanalytics.atk.engine.giraph")

    val giraphConf = new LdaConfiguration(hConf)

    val docOut = frames.prepareForSave(CreateEntityArgs(description = Some("LDA doc results")))
    val wordOut = frames.prepareForSave(CreateEntityArgs(description = Some("LDA word results")))
    val topicOut = frames.prepareForSave(CreateEntityArgs(description = Some("LDA topics given word results")))

    val inputFormatConfig = new LdaInputFormatConfig(frame.getStorageLocation, frame.schema)
    val outputFormatConfig = new LdaOutputFormatConfig(
      docOut.getStorageLocation,
      wordOut.getStorageLocation,
      topicOut.getStorageLocation
    )
    val ldaConfig = new LdaConfig(inputFormatConfig, outputFormatConfig, arguments)

    giraphConf.setLdaConfig(ldaConfig)
    GiraphConfigurationUtil.set(giraphConf, "giraphjob.maxSteps", arguments.maxIterations)
    GiraphConfigurationUtil.set(giraphConf, "mapreduce.input.fileinputformat.inputdir", Some(inputFormatConfig.parquetFileLocation))

    giraphConf.setEdgeInputFormatClass(classOf[LdaParquetFrameEdgeInputFormat])
    giraphConf.setVertexOutputFormatClass(classOf[LdaParquetFrameVertexOutputFormat])
    giraphConf.setMasterComputeClass(classOf[CVB0LDAMasterCompute])
    giraphConf.setComputationClass(classOf[CVB0LDAComputation])
    giraphConf.setAggregatorWriterClass(classOf[CVB0LDAAggregatorWriter])

    val report = GiraphJobManager.run(s"ia_giraph_lda_train_${invocation.asInstanceOf[CommandInvocation].commandId}",
      classOf[CVB0LDAComputation].getCanonicalName,
      giraphConf,
      invocation,
      "lda-learning-report_0")

    val resultsColumn = Column("topic_probabilities", DataTypes.vector(arguments.getNumTopics))

    // After saving update timestamps, status, row count, etc.
    frames.postSave(None, docOut.toReference, new FrameSchema(List(frame.schema.column(arguments.documentColumnName), resultsColumn)))
    frames.postSave(None, wordOut.toReference, new FrameSchema(List(frame.schema.column(arguments.wordColumnName), resultsColumn)))
    frames.postSave(None, topicOut.toReference, new FrameSchema(List(frame.schema.column(arguments.wordColumnName), resultsColumn)))

    val model: Model = arguments.model
    val topicFrame: SparkFrame = topicOut.toReference
    model.data = createLdaModel(topicFrame.rdd, arguments.wordColumnName, "topic_probabilities").toJson.asJsObject

    LdaTrainResult(
      frames.expectFrame(docOut.toReference),
      frames.expectFrame(wordOut.toReference),
      frames.expectFrame(topicOut.toReference),
      report)
  }

  /* Create LDA model from frame */
  private def createLdaModel(frameRdd: FrameRdd,
                             wordColumnName: String,
                             probColumnName: String): LdaModel = {
    val topicWordMap = frameRdd.mapRows(row => {
      val word = row.value(wordColumnName).toString
      val prob = row.vectorValue(probColumnName)
      (word, prob)
    }).collect().toMap

    new LdaModel(topicWordMap)
  }
}
