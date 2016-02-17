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

package org.trustedanalytics.atk.scoring.models

import org.apache.spark.mllib.ScoringModelTestUtils
import org.apache.spark.mllib.classification.SVMModel
import org.apache.spark.mllib.linalg.DenseVector
import org.scalatest.WordSpec

class SVMWithSGDModelTest extends WordSpec {
  val weights = new DenseVector(Array(2, 3))
  val intercept = 4
  val obsCols = List("a", "b", "c")
  val svmWithSGDModel = new SVMModel(weights, intercept)
  val svmWithSGDData = new SVMData(svmWithSGDModel, obsCols)
  var svmScoreModel = new SVMWithSGDScoreModel(svmWithSGDData)
  val numObsCols = obsCols.length

  "SVMWithSGDModel" should {
    "throw an exception when attempting to score null data" in {
      ScoringModelTestUtils.nullDataTest(svmScoreModel)
    }

    "throw an exception when scoring data with too few columns" in {
      ScoringModelTestUtils.tooFewDataColumnsTest(svmScoreModel, weights.size)
    }

    "throw an exception when scoring data with too many columns" in {
      ScoringModelTestUtils.tooManyDataColumnsTest(svmScoreModel, weights.size)
    }

    "throw an exception when scoring data with non-numerical records" in {
      ScoringModelTestUtils.invalidDataTest(svmScoreModel, weights.size)
    }

    "successfully score a model when float data is provided" in {
      ScoringModelTestUtils.successfulModelScoringFloatTest(svmScoreModel, weights.size)
    }

    "successfully score a model when integer data is provided" in {
      ScoringModelTestUtils.successfulModelScoringFloatTest(svmScoreModel, weights.size)
    }

    "successfully return the observation columns used for training the model" in {
      ScoringModelTestUtils.successfulInputTest(svmScoreModel, numObsCols)
    }

    "successfully return the observation columns used for training the model along with score" in {
      ScoringModelTestUtils.successfulOutputTest(svmScoreModel, numObsCols)
    }

  }
}
