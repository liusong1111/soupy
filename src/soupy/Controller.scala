package soupy

import javax.xml.soap.Node
import java.io.File
import org.jsoup.Jsoup

class Controller {
  def render(view: View, options: Map[String, Any]) {
    var result = view
    if (options.contains("layout")) {
      val layout = options("layout").asInstanceOf[String]
//      val f = new File(soupy.root + "/app/views" + layout + ".html")
//      val doc = Jsoup.parse(f, "UTF-8")
      val layoutView = new LayoutView(layout)
      layoutView.setPart(view)
//      doc.select(".yield").html(body)
//      body = doc.toString
      result = layoutView
    }

    println(result)
    result
  }

}