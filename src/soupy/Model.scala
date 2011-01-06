package soupy

class Model extends Query{

}

trait Query {
  var _from: String = _
  var _where = List[String]()

  def where(pair: Pair[String, String]) = {
    val condition = "%s = %s".format(pair._1, pair._2)
    _where = condition :: _where
  }

  def from(table: String) {
    _from = table
  }

  def toSQL = {
    var sql = new StringBuffer()
    sql.append("select *  from %s".format(_from))
    if (_where.length > 0) {
      sql.append(_where.mkString(" AND "))
    }
    sql.toString
  }

  def all = {

  }

  def first = {

  }
}
