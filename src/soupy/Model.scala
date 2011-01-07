package soupy

import collection.mutable.ListBuffer
import java.sql._
import java.beans.Introspector
import java.beans.PropertyDescriptor

class Dao[T](val modelClass: Class[T], val tableName: String) {
  def all: ListBuffer[T] = {
    q.fetch
  }

  def q = {
    val q = new Query(tableName, modelClass)

    q
  }

  def insert(m: T): Boolean = {
    q.insert(m)
  }

  def count: Int = {
    q.count
  }
}

class Query[T](var tableName: String = "",
               var modelClass: Class[T] = null) {
  var _from: String = tableName
  var _where = ListBuffer[String]()
  var _select:String = null

  def select(select: String) = {
    _select = select

    this
  }

  def from(table: String) = {
    _from = table

    this
  }

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

  def toSQL = {
    var sql = new StringBuffer()
    var select = if (_select eq null) {
      "select *"
    } else {
      _select
    }
    sql.append(select).append(" ")

    sql.append("from %s".format(_from))
    if (_where.length > 0) {
      sql.append(" where ").append(_where.mkString(" AND "))
    }
    sql.toString
  }

  def count = {
    var result = 0
    select("select count(1)").executeQuery {
      rs =>
        result = rs.getInt(1)
    }

    result
  }

  def executeQuery(callback: ResultSet => Unit): Unit = {
    usingConnection {
      conn =>
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
          try {
            rs.close
          } catch {
            case _ => None
          }
          try {
            st.close
          } catch {
            case _ => None
          }
        }
    }
  }

  def insert(m: T): Boolean = {
    var result = false
    val propertyDescriptors = Introspector.getBeanInfo(modelClass).getPropertyDescriptors
    val fNames = ListBuffer[String]()
    val values = ListBuffer[Object]()
    propertyDescriptors.filter {
      prop =>
        prop.getName != "class"
    }.foreach {
      prop =>
        fNames += prop.getName
        values += prop.getReadMethod.invoke(m)
    }

    val sFields = fNames.mkString(",")
    val sHolders = (1 to (fNames.length)).map {
      i => "?"
    }.mkString(",")
    val sql = "insert into %s(%s) values(%s)".format(_from, sFields, sHolders)
    //    println(sql)
    usingConnection {
      conn =>
        var st: PreparedStatement = null
        try {
          st = conn.prepareStatement(sql)
          values.zipWithIndex.foreach {
            zip =>
              val (value, i) = zip
              st.setObject(i + 1, value)
          }

          result = st.executeUpdate == 0
        } finally {
          try {
            st.close
          } catch {
            case _ => None
          }
        }
    }

    result
  }

  def fetch: ListBuffer[T] = {
    val result = ListBuffer[T]()
    executeQuery {
      rs =>
        val m = ModelConverter.resultSetToModel(rs, modelClass)
        result += m
    }

    result
  }

  protected
  def usingConnection(callback: Connection => Unit) {
    val conn = soupy.application.getConnection
    try {
      callback(conn)
    } finally {
      try {
        conn.close
      } catch {
        case _ => None
      }
    }
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
