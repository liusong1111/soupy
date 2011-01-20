import controllers._
import models._

object UsersControllerTest{
  def main(args: Array[String]) {
    soupy.application = MyApplication

    println(UsersController.index)
  }
}