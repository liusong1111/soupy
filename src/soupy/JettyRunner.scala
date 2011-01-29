package soupy

import org.eclipse.jetty.server.Server
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import java.sql.DriverManager

abstract class Application {
  lazy val root: String = {
    System.getProperty("user.dir")
  }

  def routes: Routes

  def run = {
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


//package soupy
//
//import java.sql.DriverManager
//import org.eclipse.jetty.webapp.WebAppContext
//import org.eclipse.jetty.server.handler.{ContextHandlerCollection}
//import org.eclipse.jetty.server.nio.SelectChannelConnector
//import java.io.IOException
//import javax.servlet.ServletException
//import org.eclipse.jetty.server.{Request, Connector, Server}
//import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
//
//abstract class Application {
//  lazy val root: String = {
//    System.getProperty("user.dir")
//  }
//
//  def routes: Routes
//
//  def run = {
//    JettyRunner.run(this)
//  }
//}
//
//class DispatcherServlet(application: Application) extends HttpServlet {
//  val root = """d:/test/soupy"""
//
//  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
//    val pathInfo = request.getPathInfo
//
//    val route = application.routes.recognize(pathInfo, request.getMethod).orNull
//    if (route eq null) {
//      throw new Exception("No routes found! [uri]" + request.getPathInfo + " [method]" + request.getMethod)
//    }
//    route.handler.process(request, response)
//  }
//}
//
//class DispatchHandler(application: Application) extends org.eclipse.jetty.server.handler.AbstractHandler{
//  override def handle( target:String,  baseRequest:Request,  request:HttpServletRequest,  response:HttpServletResponse):Unit = {
//    val pathInfo = request.getPathInfo
//
//    if(true){
//      throw new RuntimeException("aaaaa")
//    }
//    val route = application.routes.recognize(pathInfo, request.getMethod).orNull
//    if (route ne null) {
//      route.handler.process(request, response)
//      baseRequest.setHandled(true)
//    }
//  }
//}
//
//object JettyRunner {
//  def run(application: Application) = {
////    val server = new Server(8080)
////    val context = new ServletContextHandler
////    context.setContextPath("/")
////    context.addServlet(new ServletHolder(new DispatcherServlet(application)), "/*")
////    server.setHandler(context)
//    //
//    //    //---
//    //    //    context.setResourceBase(soupy.application.root + "/app/views")
//    ////    val u = this.getClass.getClassLoader.getResource("views").toExternalForm
//    //    val root = """d:/test/soupy"""
//    //    val u = root + "/app/views"
//    //    val publicDir = root + "/public"
//    //
//    //    val publicContext = new WebAppContext(u, "/")
//    //    publicContext.setResourceBase(publicDir)
//    //
//    ////    val resourceHandler = new ResourceHandler()
//    ////    resourceHandler.s
//    //    val webAppContext = new WebAppContext(u, "/")
//    //    webAppContext.setResourceBase(publicDir)
//    //    val contextHandlerCollection = new ContextHandlerCollection()
//    ////    contextHandlerCollection.setHandlers(Array(context, webAppContext))
//    //    contextHandlerCollection.setHandlers(Array( publicContext, webAppContext))
//    //    server.setHandler(contextHandlerCollection)
//    //    //---
////    server.start
////    server.join
//
//    //================================
//    val server = new Server
//
//    val connector = new SelectChannelConnector();
//    connector.setPort(Integer.getInteger("jetty.port", 8080).intValue)
//    connector.setHost("127.0.0.1")
//    server.setConnectors(Array[Connector](connector))
//
////    val root = new Context(contexts, "/co", Context.SESSIONS);
////    root.addServlet(new ServletHolder(new hello_one("Ciao")), "/*");
////
////
////    Context yetanother = new Context(contexts, "/yo", Context.SESSIONS);
////    yetanother.addServlet(new ServletHolder(new hello_two("YO!")), "/*");
//
//    val handlers = new ContextHandlerCollection()
//    val soupyHandler = new DispatchHandler(application)
//    val webapp = new WebAppContext(handlers, "d:/test/soupy/app/views", "/");
//
////    handlers.setHandlers(Array(soupyHandler,webapp))
//    handlers.setHandlers(Array(soupyHandler))
//
//    server.setHandler(handlers)
//
//    server.start
//    server.join
//  }
//}
