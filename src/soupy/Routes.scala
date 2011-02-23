package soupy

import collection.mutable.ListBuffer
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

//the handler interface that respond to a router
trait Handler {
  def process(request: HttpServletRequest, response: HttpServletResponse): Unit
}

// soupy's controller as a handler
object App {
  def apply[ControllerType <: Controller](actionName: String)(implicit m: Manifest[ControllerType]): Handler = {
    new ControllerHandler[ControllerType](actionName)
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

/*
/users
/users/:id
/users/:id.xml
/:city
/:city/activities
/activities/:year
/activities/:year-:month
/activities/:year(-:month-:date)
 */
class UriPart {
  def process(part: String): Option[Map[String, String]]
}

class ConstantPart(val part: String) extends UriPart {
  def process(inputPart: String): Option[Map[String, String]] = {
    if (inputPart == part) {
      Some(Map[String, String]())
    } else {
      None
    }
  }
}

class SimpleVariablePart(val part: String) extends UriPart {
  val key = part.substring(1)

  def process(inputPart: String): Option[Map[String, String]] = {
    Some(Map(key -> inputPart))
  }
}

class CompositeVariablePart(part: String) extends UriPart {
  val partKeys = {
    UriPattern.COMPOSITE_VARIABLE_REG.findAllIn(part).map(x => x)
  }

  val partReg = {
    UriPattern.COMPOSITE_VARIABLE_REG.replaceAllIn(inputPart, "(.*)").r
  }

  def process(inputPart: String): Option[Map[String, String]] = {
    val result = Map[String, String]()
    val values = partReg.findAllIn(inputPart).map(x => x)
    (0 until partKeys.length).foreach {
      i =>
        val key = partKeys(i)
        val value = values(i)
        result += (key -> value)
    }

    Some(result)
  }
}

object UriPattern {
  val CONSTANT_REG = """^[^:]+$""".r
  val SIMPLE_VARIABLE_REG = """^\:(\w+)$""".r
  val COMPOSITE_VARIABLE_REG = """\:(\w+)""".r
}

class UriPattern(val uri: String) {
  val parts: List[UriPart] = {
    val _parts = uri.split("/").filter(it => (it ne null) && (it != ""))
    _parts.map {
      _part =>
        _part match {
          case CONSTANT_REG => ConstantPart(_part)
          case SIMPLE_VARIABLE_REG(variable) => SimpleVariablePart(variable)
          case _ => CompositeVariablePart(_part)
        }
    }
  }

  def unapply(uri: String): Option[Map[String, String]] = {
    var _parts = uri.split("/").filter(it => (it != null && it != ""))
    if (_parts.length != parts.length) {
      None
    } else {
      var matched = false
      var result = Map[String, String]()
      (0 until _parts.length).foreach {
        i =>
          val _part = _parts(i)
          val part = parts(i)
          part.process(_part) match {
            case None => matched = false
            case Some(a) => result ++= a
          }
      }
      if (!matched) {
        None
      } else {
        Some(result)
      }
    }
  }
}

// route:代表一个url规则
case class Route(val uri: String, val method: String, val handler: Handler) {
  def recognize(request: HttpServletRequest): Option[Route] = {
    val _uri = request.getRequestURI
    val _method = request.getMethod

  }
}

// routes
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

// rich request: request wrapper
class RichRequest(val request: HttpServletRequest) {
  def params(key: String): String = {
    var result = attributes[String]("params." + key)
    if (result eq null) {
      result = request.getParameter(key)
    }
    result
  }

  def params(key: String, value: String): Unit = {
    attributes("params." + key, value)
  }

  def sessions[T](key: String): T = {
    request.getSession.getAttribute(key).asInstanceOf[T]
  }

  def sessions[T](key: String, value: T): Unit = {
    request.getSession.setAttribute(key, value)
  }

  def attributes[T](key: String): T = {
    request.getAttribute(key).asInstanceOf[T]
  }

  def attributes[T](key: String, value: T) = {
    request.setAttribute(key, value)
  }
}

object RichRequest {
  implicit def httpServletRequest2RichRequest(request: HttpServletRequest) = {
    new RichRequest(request)
  }
}