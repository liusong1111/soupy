package soupy

import collection.mutable.ListBuffer
import java.sql._
import java.beans.Introspector
import java.beans.PropertyDescriptor

class Dao[T](val modelClass: Class[T], val tableName: String) {
  def all: ListBuffer[T] = {
    val q = new Query()

    q.from(tableName).query(modelClass)
  }
}

class Query {
  var _from: String = _
  var _where = ListBuffer[String]()

  def where(pairs: Pair[String, Any]*) = {
    pairs.foreach {
      pair =>
        val (key, _value) = pair
        val value = _value match {
          case _: String => "'" + _value + "'"
          case _ => _value.toString
        }
        val condition = "%s = %s".format(pair._1, value)
        _where += condition
    }

    this
  }

  def from(table: String) = {
    _from = table

    this
  }

  def toSQL = {
    var sql = new StringBuffer()
    sql.append("select *  from %s".format(_from))
    if (_where.length > 0) {
      sql.append(" where ").append(_where.mkString(" AND "))
    }
    sql.toString
  }

  def execute(callback: ResultSet => Unit): Unit = {
    Class.forName("com.mysql.jdbc.Driver").newInstance
    val conn = DriverManager.getConnection("jdbc:mysql:///" + soupy.db("database"),
      soupy.db("user"), soupy.db("password"))
    var st: PreparedStatement = null
    var rs: ResultSet = null
    try {
      val sql = toSQL
      st = conn.prepareStatement(sql)
      rs = st.executeQuery
      while (rs.next) {
        callback(rs)
      }

    } finally {
      rs.close
      st.close
      conn.close
    }
  }

  def query[T](modelClass: Class[T]): ListBuffer[T] = {
    val result = ListBuffer[T]()
    execute {
      rs =>
        val m = ModelConverter.resultSetToModel(rs, modelClass)
        result += m
    }

    result
  }
}

object ModelConverter {
  def resultSetToModel[T](resultSet: ResultSet, modelClass: Class[T]): T = {
    val m = modelClass.newInstance
    val metaData = resultSet.getMetaData
    (1 to metaData.getColumnCount).foreach {
      columnIndex =>
        val label = metaData.getColumnLabel(columnIndex)
        val propertyDescriptors = Introspector.getBeanInfo(modelClass).getPropertyDescriptors
        val fields = propertyDescriptors.filter {
          pd =>
            pd.getName == label
        }
        if (!fields.isEmpty) {
          val field = fields(0)
          val setter = field.getWriteMethod
          val value = resultSet.getObject(columnIndex)
          setter.invoke(m, value)
        }
    }

    m
  }

}
