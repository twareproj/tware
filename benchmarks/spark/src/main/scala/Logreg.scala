/*
 * Copyright 2015 Tupleware
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import scala.math.exp

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.storage.StorageLevel._

object Logreg {
  val WARM = 5
  val ITER = 20

  def main(args: Array[String]) {
    val filename = args(0)
    val numAttrs = args(1).toInt
    val compress = args(2)
    val numParts = args(3)
    val kryo = args(4).toBoolean

    var conf = new SparkConf()
      .setAppName("logreg")
      .set("spark.rdd.compress", compress)
      .set("spark.default.parallelism", numParts)
    if (kryo) {
      conf = conf.set("spark.serializer",
                 "org.apache.spark.serializer.KryoSerializer")
    }
    val sc = new SparkContext(conf)

    val file = sc.textFile(filename, numParts.toInt)
    val d = file.map(parse _)
    if (kryo) {
      file.persist(MEMORY_ONLY_SER)
    }
    else {
      file.persist(MEMORY_ONLY)
    }

    var w = Array.fill[Float](numAttrs)(0.5f)
    var i = 0
    while (i < WARM) {
      val g = d.aggregate(new Array[Float](w.length))(train(_, _, w), add(_, _))
      var j = 0
      while (j < w.length) {
        w(j) -= g(j)
        j += 1
      }
      i += 1
    }

    w = Array.fill[Float](numAttrs)(0.5f)
    val start = System.nanoTime()
    i = 0
    while (i < ITER) {
      val g = d.aggregate(new Array[Float](w.length))(train(_, _, w), add(_, _))
      var j = 0
      while (j < w.length) {
        w(j) -= g(j)
        j += 1
      }
      i += 1
    }
    val stop = System.nanoTime()


    println("runtime: " + (stop - start))
    println("")
    println("")
    print("****   w: ( ")
    for (i <- 0 to w.length - 1) {
      print(w(i) + " ")
    }
    println(")")
    println("")
  }

  def parse(line: String): Array[Float] = {
    return line.split(',').map(_.toFloat).toArray
  }

  def add(a1: Array[Float], a2: Array[Float]): Array[Float] = {
    var i = 0
    while (i < a1.length) {
      a1(i) += a2(i)
      i += 1
    }
    return a1
  }

  def train(g: Array[Float], d: Array[Float], w: Array[Float]): Array[Float] = {
    var dot = 0.0f
    var i = 0
    while (i < w.length) {
      dot += w(i) * d(i)
      i += 1
    }
    val label = d(w.length)
    val s = (1.0f / (1.0f + exp(-label * dot)).toFloat - 1.0f) * label
    i = 0
    while (i < w.length) {
      g(i) += s * d(i)
      i += 1
    }
    return g
  }
}
