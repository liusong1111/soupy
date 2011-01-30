package models

import reflect.BeanInfo
import soupy.persistence._

//class Searchable{
//
//}


trait InformationDef extends TableDef {
  var c_id = field(StringType, "c_id")
  var b_id = field(StringType, "b_id")
  var icon = field(StringType, "icon")
  var title = field(StringType, "title")
  var src = field(StringType, "src")
  var lpath = field(StringType, "lpath")
  var search_key = field(StringType, "search_key")
}

@BeanInfo
class Information extends Model with InformationDef{

}

class InformationDao extends Schema[Information, InformationDao]("t_information") with InformationDef{
  val idProperty = IdProperty(c_id)

  def search(q:String) = {
    where(title like "%" + q + "%").limit(10)
  }
}

object Information extends InformationDao

object Searchable {
  def main(args: Array[String]) {
    Repository.setup("default", Map("adapter" -> "mysql", "database" -> "mportal", "host" -> "localhost", "user" -> "root", "password" -> ""))
    val items = Information.where(Information.title like "%周杰伦%").all
    items.foreach{ info =>
      println(info.title)
    }
  }
}

