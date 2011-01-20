object MyApplication extends soupy.Application {
  override def routes = demo.MyRoutes
  soupy.persistence.Repository.setup("default", Map("adapter" -> "mysql", "host" -> "localhost", "database" -> "soupy", "user" -> "root", "password" -> ""))

  def main(args: Array[String]) = {
    soupy.application = MyApplication
    MyApplication.run
  }
}
