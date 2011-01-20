package soupy

import collection.mutable.ListBuffer
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait Handler {
  def process(request: HttpServletRequest, response: HttpServletResponse): Unit
}

class Routes {
  var routeSet: RouteSet = new RouteSet

  def get(uri: String, handler: Handler, options: Map[String, Any] = Map.empty) {
    routeSet.push(uri, "GET", handler, options)
  }

  def post(uri: String, handler: Handler, options: Map[String, Any] = Map.empty) {
    routeSet.push(uri, "POST", handler, options)
  }

  def recognize(uri: String, method: String): Option[Route] = {
    routeSet.recognize(uri, method)
  }

  def by[ControllerType <: Controller](actionName: String)(implicit m: Manifest[ControllerType]): Handler = {
    new ControllerHandler[ControllerType](actionName)
  }
}

class RouteSet {
  val routes: ListBuffer[Route] = ListBuffer[Route]()

  def push(uri: String, method: String, handler: Handler, options: Map[String, Any]) {
    val route = new Route(uri, method, handler)
    routes += route
  }

  def recognize(uri: String, method: String): Option[Route] = {
    routes.find {
      case Route(_uri, _method, _) if (_uri == uri && _method == method) => true
      case _ => false
    }
  }
}

class ControllerHandler[C <: Controller](val action: String)(implicit val m: Manifest[C]) extends Handler {
  val controllerClass = m.erasure
  val method = controllerClass.getDeclaredMethods.filter {
    method => method.getName == action
  }(0)

  def process(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val controller = controllerClass.newInstance.asInstanceOf[C]
    prepareController(controller, request, response)
    method.invoke(controller)
  }

  protected def prepareController(controller: C, request: HttpServletRequest, response: HttpServletResponse) = {
    controller.request = request
    controller.response = response
  }
}

case class Route(val uri: String, val method: String, val handler: Handler)