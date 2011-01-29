package soupy

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

abstract class Controller {
  var request: HttpServletRequest = null
  var response: HttpServletResponse = null
  lazy val out = response.getWriter

  def render(view: View, options: Map[String, Any]): Unit = {
    var result = view
    if (options.contains("layout")) {
      val layout = options("layout").asInstanceOf[String]
      val layoutView = new LayoutView(layout)
      layoutView.setPart(view)
      result = layoutView
    }

    //    println(result)
    //    result.toString

    out.print(result.toString)
    out.close
  }

  def forward(jspPath:String) = {
    request.getRequestDispatcher(jspPath).forward(request, response)
  }

  def params(key: String): Any = {
    request.getParameter(key)
  }

  def sessions(key: String): Any = {
    request.getSession.getAttribute(key)
  }

  def sessions(key: String, value: AnyRef) = {
    request.getSession.setAttribute(key, value)
  }

}