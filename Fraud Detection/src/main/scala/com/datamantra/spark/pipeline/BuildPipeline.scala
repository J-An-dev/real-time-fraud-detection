package com.datamantra.spark.pipeline

import org.apache.log4j.Logger
import org.apache.spark.ml.PipelineStage
import org.apache.spark.ml.classification.DecisionTreeClassifier
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.feature.{VectorAssembler, OneHotEncoder, StringIndexer}
import org.apache.spark.ml.regression.DecisionTreeRegressor
import org.apache.spark.sql.types.{NumericType, StringType, StructType}

import scala.collection.mutable

/**
 * Created by kafka on 9/5/18.
 */
object BuildPipeline {

  val logger = Logger.getLogger(getClass.getName)

  def createStringIndexer(columns:List[String]) = {
    columns.map(column => {
          val stringIndexer = new StringIndexer()
          stringIndexer.setInputCol(column).setOutputCol(s"${column}_indexed")
          stringIndexer
    })
  }

  def createOneHotEncoder(columns:List[String]) = {

    columns.map(column => {
      val oneHotEncoder = new OneHotEncoder()
      oneHotEncoder.setInputCol(s"${column}_indexed").setOutputCol(s"${column}_encoded")
      oneHotEncoder
    })
  }

  def createVectorAssembler(featureColumns:List[String]) = {
    val vectorAssembler = new VectorAssembler()
    vectorAssembler.setInputCols(featureColumns.toArray).setOutputCol("features")
  }


  def createFeaturePipeline(schema:StructType, columns:List[String]):Array[PipelineStage] = {
    val featureColumns = mutable.ArrayBuffer[String]()
    val preprocessingStages = schema.fields.filter(field => columns.contains(field.name)).flatMap(field => {

      field.dataType match {
        case s: StringType => {

          val stringIndexer = new StringIndexer()
          stringIndexer.setInputCol(field.name).setOutputCol(s"${field.name}_indexed")

          val oneHotEncoder = new OneHotEncoder()
          oneHotEncoder.setInputCol(s"${field.name}_indexed").setOutputCol(s"${field.name}_encoded")

          featureColumns += (s"${field.name}_encoded")
          Array[PipelineStage](stringIndexer, oneHotEncoder)
          //Array[PipelineStage](stringIndexer)
        }

        case n: NumericType => {
          featureColumns += (field.name)
          Array.empty[PipelineStage]
        }
        case _ => {
          Array.empty[PipelineStage]
        }
      }

    })

    val vectorAssembler = new VectorAssembler()
    vectorAssembler.setInputCols(featureColumns.toArray).setOutputCol("features")

    (preprocessingStages :+ vectorAssembler)

  }


  def createStringIndexerPipeline(schema:StructType, columns:List[String]):Array[PipelineStage] = {
    val featureColumns = mutable.ArrayBuffer[String]()
    val preprocessingStages = schema.fields.filter(field => columns.contains(field.name)).flatMap(field => {

      field.dataType match {
        case s: StringType => {

          val stringIndexer = new StringIndexer()
          stringIndexer.setInputCol(field.name).setOutputCol(s"${field.name}_indexed")

          featureColumns += (s"${field.name}_indexed")
          Array[PipelineStage](stringIndexer)
        }

        case n: NumericType => {
          featureColumns += (field.name)
          Array.empty[PipelineStage]
        }
        case _ => {
          Array.empty[PipelineStage]
        }
      }

    })


    val vectorAssembler = new VectorAssembler()
    vectorAssembler.setInputCols(featureColumns.toArray).setOutputCol("features")

    (preprocessingStages :+ vectorAssembler)

  }
}
