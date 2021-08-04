# https://github.com/scalacenter/sbt-scalafix-example/blob/master/myproject/src/main/scala/a/A.scala
import scala.concurrent.Future
object a {
  implicit val x = () -> {
  for {
  n <- List(1, 2, 3)
  val inc = n + 1
} yield inc
}