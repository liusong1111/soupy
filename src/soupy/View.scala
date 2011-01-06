package soupy

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.Jsoup
import java.io.File
import scala.collection.JavaConversions._

object View {
  def cloneElement(node: Element): Element = {
    val c = new Element(node.tag, node.baseUri, node.attributes)
    val text = node.text
    c.text(text)
    for (e <- node.children.iterator) {
      c.appendChild(cloneElement(e))
    }

    c
  }
}

class View(val path: String, var fullHtml:Boolean = false) {
  lazy val doc: Document = {
    val f = new File(soupy.root + "/app/views" + path + ".html")
    Jsoup.parse(f, "UTF-8")
  }

  def select(selector: String) = {
    doc.select(selector)
  }

  def render = {
  }

  override def toString = {
    render
    if(fullHtml){
      doc.toString
    }else{
      doc.body.children
    }
  }

  implicit def nodesToView(nodes: Elements): NodeView = {
    new NodeView(nodes)
  }

  implicit def viewToString(view: View): String = {
    view.toString
  }

  implicit def nodesToString(nodes: Elements): String = {
    nodes.map {
      node =>
        node.toString
    }.mkString
  }
}

class LayoutView(override val path:String) extends View(path, true){
  def setPart(view:View, partName:String = ".yield")={
    doc.select(partName).renderWidget(view)
  }
}

class NodeView(val nodes: Elements) {
  def loop[T](models: List[T])(loopBody: (Element, T) => Unit) {
    if (nodes.isEmpty) {
      return
    }

    val node = nodes.get(0)

    models.foreach {
      model =>
        val n = View.cloneElement(node)
        node.parent.appendChild(n)
        loopBody(n, model)
    }

    // empty!(sorry for old version of jsoup)
    nodes.iterator.foreach {
      n =>
        n.remove
    }
  }

  def renderWidget(view: View) {
    view.render
    nodes.foreach {
      node =>
        view.doc.body.children.foreach{c =>
          node.parent.appendChild(c)
        }
        node.remove
    }
  }

  //  public Element cloneElement(Element original) {
  //        Element clone = new Element(original.tag(),
  //                                    original.baseUri(),
  //                                    original.attributes());
  //        String ownText = original.ownText();
  //        if(ownText != null) {
  //            clone.text(ownText);
  //        }
  //        for(Element e : original.children()) {
  //            clone.appendChild(this.cloneElement(e));
  //        }
  //        return clone;
  //    }
  //copy patch for deep clone
}
