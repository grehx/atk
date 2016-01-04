/**
 *  Copyright (c) 2015 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.trustedanalytics.atk.engine.frame.plugins.load.HivePlugin

import org.trustedanalytics.atk.domain.frame.FrameEntity
import org.trustedanalytics.atk.domain.frame.load.{ HiveArgs }
import org.trustedanalytics.atk.engine.frame.SparkFrame
import org.trustedanalytics.atk.engine.frame.plugins.load.LoadRddFunctions
import org.trustedanalytics.atk.engine.plugin.{ Invocation, PluginDoc, SparkCommandPlugin }

import spray.json._
import org.trustedanalytics.atk.domain.DomainJsonProtocol._
/**
 * Parsing data to load and append to data frames
 */
@PluginDoc(oneLine = "Append data from a hive table into an existing (possibly empty) frame",
  extended = "Append data from a hive table into an existing (possibly empty) frame",
  returns = "the initial frame with the hive data appended")
class LoadFromHivePlugin extends SparkCommandPlugin[HiveArgs, FrameEntity] {

  /**
   * The name of the command, e.g. graph/loopy_belief_propagation
   *
   * The format of the name determines how the plugin gets "installed" in the client layer
   * e.g Python client via code generation.
   */
  override def name: String = "frame/_loadhive"

  /**
   * Number of Spark jobs that get created by running this command
   * (this configuration is used to prevent multiple progress bars in Python client)
   */
  override def numberOfJobs(load: HiveArgs)(implicit invocation: Invocation) = 8

  /**
   * Parsing data to load and append to data frames
   *
   * @param invocation information about the user and the circumstances at the time of the call,
   *                   as well as a function that can be called to produce a SparkContext that
   *                   can be used during this invocation.
   * @param arguments the arguments supplied by the caller
   * @return a value of type declared as the Return type.
   */
  override def execute(arguments: HiveArgs)(implicit invocation: Invocation): FrameEntity = {
    val destinationFrame: SparkFrame = arguments.destination
    val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
    val rdd = sqlContext.sql(arguments.query)
    val additionalData = LoadHiveImpl.hiveFrameToFrameRdd(rdd)

    LoadRddFunctions.unionAndSave(destinationFrame, additionalData)
  }

}
