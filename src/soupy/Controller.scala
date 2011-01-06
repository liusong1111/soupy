package soupy

import javax.xml.soap.Node
import java.io.File
import org.jsoup.Jsoup

class Controller {
  def render(view: View, options: Map[String, Any]) {
    var body = view.toString

    println("body^^^^")
    println(body)
    println("^^^end body")

    if (options.contains("layout")) {
      val layout = options("layout")
      val f = new File(soupy.root + "\\app\\views" + "\\users\\" + layout + ".html")
      val doc = Jsoup.parse(f, "UTF-8")

      doc.select(".yield").html(body)
      body = doc.toString
    }

    println(body)
    body

  }
}