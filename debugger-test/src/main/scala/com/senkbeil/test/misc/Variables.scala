package com.senkbeil.test.misc

/**
 * Provides test of examining variable values.
 *
 * @note This was lifted from ensime-server, which in turn lifted it from
 *       the Scala IDE project!
 *
 * @note Should have a class name of com.senkbeil.test.misc.Variables
 */
object Variables {
  def main(args: Array[String]) {
    val a = true
    val b = 'c'
    val c = 3.asInstanceOf[Short]
    val d = 4
    val e = 5L
    val f = 1.0f
    val g = 2.0
    val h = "test"
    val i = Array(1, 2, 3)
    val j = List(4, 5, 6)
    val k = Array(One("one"), 1, true)
    val l = NullToString

    noop(None)
  }

  case class One(s: String) { override def toString = s }

  object NullToString { override def toString = null }

  /** No-op. */
  def noop[T](a: T): T = a
}
