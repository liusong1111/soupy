package soupy

import org.jsoup.nodes.Element
import org.jsoup.Jsoup
import java.io.File

class View(val action:String){
  lazy val doc:Element = {
    val f = new File(soupy.root + "\\app\\views" + "\\users\\" + action + ".html")
    Jsoup.parse(f, "UTF-8")
  }

  def find(selector:String)={
    doc.select(selector)
  }

  override def toString={
    doc.toString
  }
}