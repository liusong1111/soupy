package soupy

import org.eclipse.jetty.server.Server
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import java.sql.DriverManager

abstract class Application {
  lazy val root:String = {
    System.getProperty("user.dir")
  }
  def db:Map[String,String]
  def routes:Routes

  def getConnection = {
    Class.forName("com.mysql.jdbc.Driver").newInstance
    val conn = DriverManager.getConnection("jdbc:mysql:///" + db("database"),
      db("user"), db("password"))

    conn
  }

  def run={
    JettyRunner.run(this)
  }
}

class DispatcherServlet(application: Application) extends HttpServlet {
  override
  def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    //    val writer = response.getWriter
    //    writer.println("OK--11!")
    //    writer.close
    val route = application.routes.recognize(request.getPathInfo, request.getMethod).orNull
    if (route eq null) {
      throw new Exception("No routes found! [uri]" + request.getPathInfo + " [method]" + request.getMethod)
    }
    route.handler.process(request, response)
  }
}

object JettyRunner {
  def run(application: Application) = {
    val server = new Server(8080)
    val context = new ServletContextHandler
    context.setContextPath("/")
    server.setHandler(context)
    context.addServlet(new ServletHolder(new DispatcherServlet(application)), "/*")
    server.start
    server.join
  }
}
