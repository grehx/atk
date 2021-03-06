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

package org.trustedanalytics.atk.engine.model.plugins.dimensionalityreduction

import breeze.numerics._
import org.apache.spark.mllib.atk.plugins.MLLibJsonProtocol
import org.trustedanalytics.atk.domain.frame._
import org.trustedanalytics.atk.domain.schema.{ DataTypes, Schema }
import org.trustedanalytics.atk.engine.frame.SparkFrame
import org.trustedanalytics.atk.engine.model.Model
import org.trustedanalytics.atk.engine.plugin.{ Invocation, PluginDoc }
import org.trustedanalytics.atk.engine.plugin.{ SparkCommandPlugin, SparkInvocation }
import org.trustedanalytics.atk.domain.schema.DataTypes.vector
import org.apache.spark.mllib.linalg.Matrix
import org.apache.spark.mllib.linalg.distributed.RowMatrix

// Implicits needed for JSON conversion
import spray.json._
import org.trustedanalytics.atk.domain.DomainJsonProtocol._
import MLLibJsonProtocol._

@PluginDoc(oneLine = "Build principal components model.",
  extended = """Creating a PrincipalComponents Model using the observation columns.""",
  returns =
    """Values of the  principal components model object storing:
    | principal components count used to train the model,
    | the list of observation columns on which the model was trained,
    | the singular values vector and the vFactor matrix stored as an array of double values.
  """.stripMargin)
class PrincipalComponentsTrainPlugin extends SparkCommandPlugin[PrincipalComponentsTrainArgs, PrincipalComponentsTrainReturn] {

  /**
   * The name of the command
   */
  override def name: String = "model:principal_components/train"

  /**
   * Number of Spark jobs that get created by running this command
   * (this configuration is used to prevent multiple progress bars in Python client)
   */
  override def numberOfJobs(arguments: PrincipalComponentsTrainArgs)(implicit invocation: Invocation) = 7

  /**
   * Calculate principal components for the specified columns
   *
   * @param invocation information about the user and the circumstances at the time of the call, as well as a function
   *                   that can be called to produce a SparkContext that can be used during this invocation
   * @param arguments input specification for covariance matrix
   * @return value of type declared as the Return type
   */
  override def execute(arguments: PrincipalComponentsTrainArgs)(implicit invocation: Invocation): PrincipalComponentsTrainReturn = {
    val model: Model = arguments.model
    val frame: SparkFrame = arguments.frame

    validatePrincipalComponentsArgs(frame.schema, arguments)

    val k = arguments.k.getOrElse(arguments.observationColumns.length)
    val rowMatrix: RowMatrix = new RowMatrix(frame.rdd.toVectorDenseRDD(arguments.observationColumns))

    val svd = rowMatrix.computeSVD(k, computeU = true)

    val jsonModel = new PrincipalComponentsData(k, arguments.observationColumns, svd.s, svd.V)
    model.data = jsonModel.toJson.asJsObject

    new PrincipalComponentsTrainReturn(jsonModel)
  }

  // TODO: this kind of standardized validation belongs in the Schema class
  // Validate input arguments
  private def validatePrincipalComponentsArgs(frameSchema: Schema, arguments: PrincipalComponentsTrainArgs): Unit = {
    val dataColumnNames = arguments.observationColumns
    if (dataColumnNames.size == 1) {
      frameSchema.requireColumnIsType(dataColumnNames.toList.head, DataTypes.isVectorDataType)
    }
    else {
      require(dataColumnNames.size >= 2, "single vector column, or two or more numeric columns required")
      frameSchema.requireColumnsOfNumericPrimitives(dataColumnNames)
    }
    require(arguments.k.getOrElse(arguments.observationColumns.length) >= 1, "k should be greater than equal to 1")
  }

}
