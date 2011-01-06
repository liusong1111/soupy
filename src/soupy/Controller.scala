package soupy

class Controller {
  def render(view: View, options: Map[String, Any]):String = {
    var result = view
    if (options.contains("layout")) {
      val layout = options("layout").asInstanceOf[String]
      val layoutView = new LayoutView(layout)
      layoutView.setPart(view)
      result = layoutView
    }

//    println(result)
    result.toString
  }

}