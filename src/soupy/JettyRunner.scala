package soupy

import org.eclipse.jetty.server.Server
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}

class DispatcherServlet extends HttpServlet {
  override
  def doGet (request: HttpServletRequest, response: HttpServletResponse):Unit = {
//    val writer = response.getWriter
//    writer.println("OK--11!")
//    writer.close
    val route = demo.MyRoutes.recognize(request.getPathInfo, request.getMethod).orNull
    if(route eq null){
      throw new Exception("No routes found! [uri]" + request.getPathInfo + " [method]" + request.getMethod)
    }
    route.handler.process(request, response)
  }
}

object JettyRunner {
  def main(args: Array[String]) {
    val server = new Server(8080)
    val context = new ServletContextHandler
    context.setContextPath("/")
    server.setHandler(context)
    context.addServlet(new ServletHolder(new DispatcherServlet()), "/*")
    server.start
    server.join
  }
}