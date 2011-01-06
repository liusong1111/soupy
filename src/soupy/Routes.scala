package soupy

import collection.mutable.ListBuffer
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class Routes{
  var routeSet:RouteSet = new RouteSet

  def get(uri:String, options: Map[String, Any] = Map.empty){
    routeSet.push(uri, "GET", options)
  }

  def post(uri:String, options: Map[String, Any] = Map.empty){
    routeSet.push(uri, "POST", options)
  }

  def recognize(uri:String, method:String):Option[Route]={
    routeSet.recognize(uri, method)
  }
}

class RouteSet{
  val routes:ListBuffer[Route] = ListBuffer[Route]()
  def push(uri:String, method:String, options:Map[String, Any]){
    val handler = Handler(options)
    val route = new Route(uri, method, handler)
    routes += route
  }

  def recognize(uri:String, method:String):Option[Route]={
    routes.find{route =>
      route match{
        case Route(_uri, _method, _) if(_uri == uri && _method == method) => true
        case _ => false
      }
    }
  }
}

object Handler{
  def apply(options:Map[String, Any])={
    val handler = new Handler(options("controller").asInstanceOf[Controller], options("action").asInstanceOf[String])
    handler
  }
}

class Handler(val controller:Controller, val action:String){
  def process(request:HttpServletRequest, response:HttpServletResponse){
    val method = controller.getClass.getDeclaredMethods.filter{meth => meth.getName == action}(0)
    val reply = method.invoke(controller)
    val writer = response.getWriter
    writer.print(reply)
    writer.close
  }
}

case class Route(val uri:String, val method:String, val handler:Handler)