//package soupy
//
//import collection.mutable.ListBuffer
//import java.sql._
//import java.beans.Introspector
//import java.beans.PropertyDescriptor
//
//class Dao[T](val modelClass: Class[T], val tableName: String) {
//  def all: ListBuffer[T] = {
//    q.fetch
//  }
//
//  def q = {
//    val q = new Query(tableName, modelClass)
//
//    q
//  }
//
//  def insert(m: T): Boolean = {
//    q.insert(m)
//  }
//
//  def count: Int = {
//    q.count
//  }
//}
//
//trait SQLExecutor[T] {
//  def executeQuery(sql: String): ListBuffer[T] = {
//    executeQuery(sql, null, callback)
//  }
//
//  def executeQuery(sql: String, params: Seq[Object]): ListBuffer[T] = {
//    val result = ListBuffer[T]()
//    rawExecuteQuery(sql, params) {
//      rs =>
//        result += ModelConverter.resultSetToModel(rs, modelClass)
//    }
//
//    result
//  }
//
//  def rawExecuteQuery(sql: String, callback: ResultSet => T): Unit = {
//    rawExecuteQuery(sql, null, callback)
//  }
//
//  def rawExecuteQuery(sql: String, params: Seq[Object], callback: ResultSet => Unit): Unit = {
//    usingConnection {
//      conn =>
//        var st: PreparedStatement = null
//        var rs: ResultSet = null
//        try {
//          st = conn.prepareStatement(sql)
//          if (params ne null && !params.isEmpty) {
//            params.zipWithIndex.foreach {
//              (param, i) =>
//                st.setObject(i + 1, param)
//            }
//          }
//          rs = st.executeQuery
//          while (rs.next) {
//            callback(rs)
//          }
//
//        } finally {
//          try {
//            rs.close
//          } catch {
//            case _ => None
//          }
//          try {
//            st.close
//          } catch {
//            case _ => None
//          }
//        }
//    }
//  }
//
//  def usingConnection(callback: Connection => Unit) {
//    val conn = soupy.application.getConnection
//    try {
//      callback(conn)
//    } finally {
//      try {
//        conn.close
//      } catch {
//        case _ => None
//      }
//    }
//  }
//}
//
//class OperatorEnum extends Enumeration {
//  val eq, ne, in, between, gt, lt, gte, lte = Value
//}
//
//class Operator(val op:OperatorEnum) {
//  def toSQL(f:String, v:String) = {
//    ("%s %s ?".format(f, op), v)
//  }
//
//  def encode(v: List[Object]) {
//    v.mkString(",").wrap("(", ")")
//  }
//}
//
//class Eq extends Operator("=")
//
//class Ne extends Operator("<>")
//
//class Gt extends Operator(">")
//
//class Lt extends Operator("<")
//
//class Gte extends Operator(">=")
//
//class Lte extends Operator("<=")
//
//object Where {
//  def apply(fieldName: String, operator: Operator, fieldValue: Object) {
//    if (fieldValue.isInstanceOf[List] && operator != Operator.between) {
//      operator = Operator.in
//    }
//
//    new Where(fieldName, operator, fieldValue)
//  }
//
//  def apply(fieldName: String, fieldValue: Object) {
//    Where(fieldName, Operator.eq, fieldValue)
//  }
//
//  def apply(pair: Pair[String, Any]) {
//    Where(pair._1, Operator.eq, pair._2)
//  }
//
//  def apply(pairs: (Pair[String, Any] | Where)*) = {
//    And(pairs.map {
//      pair => Where(pair)
//    })
//  }
//
//
//  //  def apply(pairs: Pair[String, Any]*) = {
//  //    pairs.foreach {
//  //      pair =>
//  //        val (key, _value) = pair
//  //        val value = _value match {
//  //          case _: String => "'" + _value + "'"
//  //          case _ => _value.toString
//  //        }
//  //        val condition = "%s = %s".format(pair._1, value)
//  //        _where += condition
//  //    }
//  //  }
//}
//
//class And(var wheres: Where*) {
//  def toSQL = {
//    wheres.map {
//      where => where.toSQL
//    }.mkString(" AND ")
//  }
//}
//
//object And {
//  def apply(wheres: Where*) {
//    new And(wheres: _*)
//  }
//}
//
//class Or(var wheres: Where*) {
//  def toSQL = {
//    wheres.map {
//      where => where.toSQL
//    }.mkString(" OR ")
//  }
//}
//
//object Or {
//  def apply(wheres: Where*) {
//    new Or(wheres: _*)
//  }
//}
//
//class Where(val fieldName: String, val operator: Operator, val values: Object) {
//  def toSQL = {
//    operator.toSQL(fieldName, values)
//  }
//}
//
//class Query[T](var tableName: String = "",
//               var modelClass: Class[T] = null) extends SQLExecutor[T] {
//  var _from: String = null
//  var _where = ListBuffer[String]()
//  var _select: String = null
//
//  def select(select: String) = {
//    _select = select
//
//    this
//  }
//
//  def from(table: String) = {
//    _from = table
//
//    this
//  }
//
//  def where(pairs: Pair[String, Any]*) = {
//    Where(pairs: _*)
//
//    this
//  }
//
//  def toSQL = {
//    var sql = new StringBuffer()
//    var params = ListBuffer[Object]()
//
//    _select = if (_select eq null) "select *" else _select
//    _from = if (_from eq null) tableName else _from
//
//    sql.append(_select).append(" ")
//    sql.append("from %s".format(_from))
//    if (_where.length > 0) {
//      //      sql.append(" where ").append(_where.mkString(" AND "))
//      sql.append(" where ").append(_where.mkString(" AND "))
//    }
//    sql.toString
//  }
//
//  def count = {
//    var result = 0
//    val sql = select("select count(1)").toSQL
//    SQLExecutor.executeQuery(sql) {
//      rs =>
//        result = rs.getInt(1)
//    }
//
//    result
//  }
//
//
//  def insert(m: T): Boolean = {
//    var result = false
//    val propertyDescriptors = Introspector.getBeanInfo(modelClass).getPropertyDescriptors
//    val fNames = ListBuffer[String]()
//    val values = ListBuffer[Object]()
//    propertyDescriptors.filter {
//      prop =>
//        prop.getName != "class"
//    }.foreach {
//      prop =>
//        fNames += prop.getName
//        values += prop.getReadMethod.invoke(m)
//    }
//
//    val sFields = fNames.mkString(",")
//    val sHolders = (1 to (fNames.length)).map {
//      i => "?"
//    }.mkString(",")
//    val sql = "insert into %s(%s) values(%s)".format(tableName, sFields, sHolders)
//    //    println(sql)
//    usingConnection {
//      conn =>
//        var st: PreparedStatement = null
//        try {
//          st = conn.prepareStatement(sql)
//          values.zipWithIndex.foreach {
//            zip =>
//              val (value, i) = zip
//              st.setObject(i + 1, value)
//          }
//
//          result = st.executeUpdate == 0
//        } finally {
//          try {
//            st.close
//          } catch {
//            case _ => None
//          }
//        }
//    }
//
//    result
//  }
//
//  def fetch: ListBuffer[T] = {
//    val result = ListBuffer[T]()
//    SQLExecutor.executeQuery(toSQL) {
//      rs =>
//        val m = ModelConverter.resultSetToModel(rs, modelClass)
//        result += m
//    }
//
//    result
//  }
//
//}
//
//
//object ModelConverter {
//  def resultSetToModel[T](resultSet: ResultSet, modelClass: Class[T]): T = {
//    val m = modelClass.newInstance
//    val metaData = resultSet.getMetaData
//    (1 to metaData.getColumnCount).foreach {
//      columnIndex =>
//        val label = metaData.getColumnLabel(columnIndex)
//        val propertyDescriptors = Introspector.getBeanInfo(modelClass).getPropertyDescriptors
//        val fields = propertyDescriptors.filter {
//          pd =>
//            pd.getName == label
//        }
//        if (!fields.isEmpty) {
//          val field = fields(0)
//          val setter = field.getWriteMethod
//          val value = resultSet.getObject(columnIndex)
//          setter.invoke(m, value)
//        }
//    }
//
//    m
//  }
//
//}

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

