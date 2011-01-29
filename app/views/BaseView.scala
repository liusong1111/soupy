package views

import xml.Elem
import java.io.PrintWriter

object BaseView{
  implicit def view2Elem(view:BaseView):Elem = {
    view.render
  }
}
abstract class BaseView{
  import BaseView._
  def render:Elem

  var requiredJavascripts = List[String]()
  var requiredStylesheets = List[String]()

  def requireJavascript(jsFiles:String*){
    requiredJavascripts = requiredJavascripts ::: jsFiles.toList
  }

  def requireCss(cssFiles:String*){
    requiredStylesheets = requiredStylesheets ::: cssFiles.toList
  }

  def output(out:PrintWriter):Unit = {
    out.println(render.toString)
  }

//  override def toString = {
//    render.toString
//  }
}