object MyApplication extends soupy.Application {
  override def routes = demo.MyRoutes
  override def db = Map("database" -> "soupy", "host" -> "localhost", "user" -> "root", "password" -> "")

  def main(args: Array[String]) = {
    soupy.application = MyApplication
    MyApplication.run
  }
}
