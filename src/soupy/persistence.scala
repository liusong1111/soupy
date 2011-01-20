package soupy.persistence

import java.sql.{DriverManager, Connection, ResultSet, PreparedStatement}
import java.beans.Introspector
import reflect.BeanInfo
import org.apache.commons.logging.LogFactory

package object persistence {
  val logger = LogFactory.getLog("persistence")
}

//----------- SQL Related -------------
//--------Order --------------
trait Order {
  def toSQL: String

  override def toString = toSQL

  def &&(anotherOrder: Order) = {
    new CompositeOrder(this.orders ::: anotherOrder.orders)
  }

  val orders: List[SimpleOrder]
}

trait SimpleOrder extends Order {
  val orders: List[SimpleOrder] = List(this)
}

class DescOrder[A](val prop: Property[A]) extends SimpleOrder {
  override def toSQL = prop.name + " DESC"
}

class AscOrder[A](val prop: Property[A]) extends SimpleOrder {
  override def toSQL = prop.name + " ASC"
}

class CompositeOrder(override val orders: List[SimpleOrder]) extends Order {
  override def toSQL = orders.map {
    order => order.toSQL
  }.mkString(",")
}

//-------------criteria----------------
trait Criteria {
  def toSQL: String

  override def toString = toSQL

  def where(criteria: Criteria) = {
    new AndCriteria(this, criteria)
  }

  def &&(criteria: Criteria) = {
    where(criteria)
  }

  def ||(criteria: Criteria) = {
    new OrCriteria(this, criteria)
  }
}

trait SimpleCriteria extends Criteria

class NormalCriteria[A](val prop: Property[A], val op: String, val value: A) extends SimpleCriteria {
  override def toSQL = prop.name + " " + op + " " + prop.encode(value)
}

class LikeCriteria(val prop: Property[String], val value: String) extends SimpleCriteria {
  override def toSQL = prop.name + " like " + prop.encode(value)
}

class IsNullCriteria[A](val prop: Property[A]) extends SimpleCriteria {
  override def toSQL = prop.name + " is null"
}

class IsNotNullCriteria[A](val prop: Property[A]) extends SimpleCriteria {
  override def toSQL = prop.name + " is not null"
}

class InCriteria[A](val prop: Property[A], val values: List[A]) extends SimpleCriteria {
  override def toSQL = {
    if (!values.isEmpty) {
      prop.name + " in (" + values.map {
        value => prop.encode(value)
      }.mkString(",") + ")"
    } else {
      " 1=1 "
    }
  }
}

class RawCriteria(val sqlTemplate: String) extends SimpleCriteria {
  override def toSQL = sqlTemplate
}

//TODO: missing toSQL implementation
class ListRawCriteria(override val sqlTemplate: String, val args: List[Any]) extends RawCriteria(sqlTemplate) {

}

//TODO: missing toSQL implementation
class MapRawCriteria(override val sqlTemplate: String, val args: Map[Any, Any]) extends RawCriteria(sqlTemplate) {

}

trait CompositeCriteria extends Criteria

class AndCriteria(val criterias: Criteria*) extends CompositeCriteria {
  override
  def toSQL = {
    criterias.map {
      criteria => if (criteria.isInstanceOf[OrCriteria]) "(" + criteria.toSQL + ")" else criteria.toSQL
    }.mkString(" AND ")
  }
}

class OrCriteria(val criterias: Criteria*) extends CompositeCriteria {
  override
  def toSQL = {
    criterias.map {
      criteria => if (criteria.isInstanceOf[AndCriteria]) "(" + criteria.toSQL + ")" else criteria.toSQL
    }.mkString(" OR ")
  }
}

//------ properties---------------------
trait PropertyOperations[A] {
  self: Property[A] =>

  //criteria
  def >(value: A) = new NormalCriteria(this, ">", value)

  def >=(value: A) = new NormalCriteria(this, ">=", value)

  def <(value: A) = new NormalCriteria(this, "<", value)

  def <=(value: A) = new NormalCriteria(this, "<=", value)

  def ==(value: A) = new NormalCriteria(this, "=", value)

  def !=(value: A) = new NormalCriteria(this, "<>", value)

  def isNull = new IsNullCriteria(this)

  def isNotNull = new IsNotNullCriteria(this)

  def in(values: List[A]) = new InCriteria(this, values)

  //order
  def asc = new AscOrder(this)

  def desc = new DescOrder(this)
}

trait PropertyWorker[A] {
  self: Property[A] =>
  def get(m: Object): A = {
    getter.invoke(m).asInstanceOf[A]
  }

  def set(m: Object, v: A) = {
    setter.invoke(m, v.asInstanceOf[Object])
  }

  lazy val getter = {
    val propertyDescriptors = Introspector.getBeanInfo(modelClass).getPropertyDescriptors
    propertyDescriptors.filter {
      prop =>
        prop.getName == self.name
    }.head.getReadMethod
  }
  lazy val setter = {
    val propertyDescriptors = Introspector.getBeanInfo(modelClass).getPropertyDescriptors
    propertyDescriptors.filter {
      prop =>
        prop.getName == self.name
    }.head.getWriteMethod
  }


  def read(rs: ResultSet, index: Int = this.index): A

  def write(ps: PreparedStatement, value: A, index: Int = this.index)
}

//value or property initializer
trait TypeBuilder[T] {
  def apply(name: String, index: Int): T
}

abstract class PropertyTypeBuilder[T] extends TypeBuilder[T] {

}

trait ValueTypeBuilder[T] extends TypeBuilder[T] {

}

//base class for property

abstract class Property[A](val modelClass: Class[_], val name: String, val index: Int) extends PropertyOperations[A] with PropertyWorker[A] {
  def encode(value: A): String = {
    value.toString
  }
}

//for String
class StringProperty(override val modelClass: Class[_], override val name: String, override val index: Int) extends Property[String](modelClass, name, index) {
  val singleQuoteRegexp = """'""".r

  override
  def encode(value: String) = {
    "'" + singleQuoteRegexp.replaceAllIn(value, "''") + "'"
  }

  override def read(rs: ResultSet, index: Int = this.index): String = {
    rs.getString(index)
  }

  override def write(ps: PreparedStatement, value: String, index: Int = this.index) = {
    ps.setString(index, value)
  }

  def like(value: String) = {
    new LikeCriteria(this, value)
  }
}

class StringPropertyBuilder[M](implicit val manifest: Manifest[M]) extends PropertyTypeBuilder[StringProperty] {
  val modelClass = manifest.erasure

  override def apply(name: String, index: Int): StringProperty = {
    new StringProperty(modelClass, name, index)
  }
}

object StringValueBuilder extends ValueTypeBuilder[String] {
  def apply(name: String, index: Int): String = {
    ""
  }
}

//for Int
class IntProperty(override val modelClass: Class[_], override val name: String, override val index: Int) extends Property[Int](modelClass, name, index) {
  override def read(rs: ResultSet, index: Int = this.index): Int = {
    rs.getInt(index)
  }

  override def write(ps: PreparedStatement, value: Int, index: Int = this.index) = {
    ps.setInt(index, value)
  }
}

class IntPropertyBuilder[M](implicit val manifest: Manifest[M]) extends PropertyTypeBuilder[IntProperty] {
  val modelClass = manifest.erasure

  override def apply(name: String, index: Int): IntProperty = {
    new IntProperty(modelClass, name, index)
  }
}

object IntValueBuilder extends ValueTypeBuilder[Int] {
  override def apply(name: String, index: Int): Int = {
    0
  }
}

//type definitions
trait TableDef {
  type StringType
  type IntType

  val StringType: TypeBuilder[StringType]
  val IntType: TypeBuilder[IntType]

  def field[T](builder: TypeBuilder[T], name: String, options: Pair[String, String]*): T
}

abstract class PropertiesDef[M](implicit val manifest: Manifest[M]) extends TableDef {
  val modelClass = manifest.erasure

  type StringType = StringProperty

  lazy val StringType: TypeBuilder[StringProperty] = new StringPropertyBuilder[M]

  type IntType = IntProperty
  lazy val IntType: TypeBuilder[IntProperty] = new IntPropertyBuilder[M]

  var properties = List[Property[Any]]()

  override def field[T](builder: TypeBuilder[T], name: String, options: Pair[String, String]*): T = {
    val index = (properties.length + 1)
    val prop = builder(name, index)
    properties = properties ::: List(prop.asInstanceOf[Property[Any]])
    prop
  }

  def propertiesWithoutId = {
    properties.filter {
      idProperty.property ne _
    }
  }

  val idProperty: IdProperty
}

class IdProperty(val property: Property[Any])

object IdProperty {
  def apply(_property: Any, idPropertyType: String = null) = {
    val property = _property.asInstanceOf[Property[Any]]
    idPropertyType match {
      case "autoIncrement" => new AutoIncrementIdProperty(property)
      case "normal" => new NormalIdProperty(property)
      case _ => new DefaultIdProperty(property)
    }
  }
}

class DefaultIdProperty(override val property: Property[Any]) extends IdProperty(property)

class AutoIncrementIdProperty(override val property: Property[Any]) extends IdProperty(property)

class NormalIdProperty(override val property: Property[Any]) extends IdProperty(property)

trait AccessorsDef extends TableDef {
  type StringType = String
  val StringType: TypeBuilder[String] = StringValueBuilder
  type IntType = Int
  val IntType: TypeBuilder[Int] = IntValueBuilder

  private var _indexCounter = 0

  override def field[T](builder: TypeBuilder[T], name: String, options: Pair[String, String]*): T = {
    _indexCounter += 1
    builder(name, _indexCounter)
  }
}

trait QueryDelegator[DAO] {
  self: Schema[_, _] =>

  def from(_from: String): DAO = {
    this.dup(query.from(_from))
  }

  def select(_select: String): DAO = {
    this.dup(query.select(_select))
  }

  def join(_join: String): DAO = {
    this.dup(query.join(_join))
  }

  def group(_group: String): DAO = {
    this.dup(query.group(_group))
  }

  def having(_having: String): DAO = {
    this.dup(query.having(_having))
  }

  def offset(_offset: Int): DAO = {
    this.dup(query.offset(_offset))
  }

  def limit(_limit: Int): DAO = {
    this.dup(query.limit(_limit))
  }

  // composite order
  def order(_order: Order): DAO = {
    this.dup(query.order(_order))
  }

  // composite criteria
  def where(_where: Criteria): DAO = {
    this.dup(query.where(_where))
  }

  def &&(_where: Criteria): DAO = {
    this.dup(query.where(_where))
  }

  def ||(_where: Criteria): DAO = {
    this.dup(query || _where)
  }

  def toSQL = {
    val q = refineQuery(query)
    q.toSQL
  }

  override def toString = toSQL

  // if didn't specify select part, populate field names to combine the statement
  def refineQuery(query: Query): Query = {
    if (query._select.isEmpty) {
      val _select = "select " + properties.map {
        prop => prop.name
      }.mkString(", ")
      query.select(_select)
    } else {
      query
    }

  }


}

//TODO: you need define IdColumn
trait ModifyDelegator[M] {
  self: Schema[_, _] =>
  def insert(m: M) = {
    val tableName = self.query._from
    val strFields = propertiesWithoutId.map {
      prop => prop.name
    }.mkString(", ")
    val strValues = propertiesWithoutId.map {
      prop => val v = prop.get(m.asInstanceOf[Object]); prop.encode(v)
    }.mkString(", ")

    new Insert(tableName, strFields, strValues).executeUpdate
  }

  def update(m: M) = {
    val tableName = self.query._from
    val sets = propertiesWithoutId.map {
      prop => prop.name + " = " + prop.encode(prop.get(m.asInstanceOf[Object]))
    }.mkString(", ")
    val criteria = new NormalCriteria(idProperty.property, "=", idProperty.property.get(m.asInstanceOf[Object]))
    new Update(tableName, sets, Some(criteria)).executeUpdate
  }

  def delete(m: M) = {
    val tableName = self.query._from
    val criteria = new NormalCriteria(idProperty.property, "=", idProperty.property.get(m.asInstanceOf[Object]))
    new Delete(tableName, Some(criteria)).executeUpdate
  }

}

abstract class Schema[M, DAO <: Schema[_, _]](val tableName: String)(override implicit val manifest: Manifest[M], implicit val daoManifest: Manifest[DAO]) extends PropertiesDef[M] with QueryDelegator[DAO] with ModifyDelegator[M] with Cloneable {
  var query: Query = new Query(_from = tableName)

  def build: M = {
    val m = modelClass.newInstance

    m.asInstanceOf[M]
  }

  def all: List[M] = {
    var result = List[M]()
    executeQuery(query) {
      rs =>
        val m = rs2M(rs)
        result = m :: result
    }

    result.reverse
  }

  def first: Option[M] = {
    var result: Option[M] = None
    executeQuery(this.limit(1).query) {
      rs =>
        result = Some(rs2M(rs))
    }

    result
  }

  def count: Int = {
    var result = 0
    refineQuery(query.select("select count(1)")).executeQuery {
      rs =>
        result = rs.getInt(1)
    }

    result
  }

  implicit def rs2M(rs: ResultSet): M = {
    val m = build
    properties.foreach {
      prop =>
        val value = prop.read(rs)
        prop.set(m.asInstanceOf[Object], value)
    }
    m
  }

  def executeQuery(query: Query = this.query)(callback: ResultSet => Unit) = {
    refineQuery(query).executeQuery(callback)
  }

  def dup[DAO <: Schema[_, _]](query: Query): DAO = {
    val ins = this.clone.asInstanceOf[DAO]
    ins.query = query
    ins
  }
}

trait Model extends AccessorsDef

//------ Query --------------------
case class Query(val _from: String = null,
                 val _select: Option[String] = None,
                 val _join: Option[String] = None,
                 val _where: Option[Criteria] = None,
                 val _order: Option[Order] = None,
                 val _group: Option[String] = None,
                 val _having: Option[String] = None,
                 val _offset: Option[Int] = None,
                 val _limit: Option[Int] = None) {
  def from(_from: String): Query = {
    this.copy(_from = _from)
  }

  def select(_select: String): Query = {
    this.copy(_select = Some(_select))
  }

  def join(_join: String): Query = {
    this.copy(_join = Some(_join))
  }

  def group(_group: String): Query = {
    this.copy(_group = Some(_group))
  }

  def having(_having: String): Query = {
    this.copy(_group = Some(_having))
  }

  def offset(_offset: Int): Query = {
    this.copy(_offset = Some(_offset))
  }

  def limit(_limit: Int): Query = {
    this.copy(_limit = Some(_limit))
  }

  // composite order
  def order(_order: Order): Query = {
    val the_order = if (this._order.isEmpty) _order else (this._order.get && _order)
    this.copy(_order = Some(the_order))
  }

  // composite criteria
  def where(_where: Criteria): Query = {
    val the_where = if (this._where.isEmpty) {
      _where
    } else {
      this._where.get && _where
    }

    this.copy(_where = Some(the_where))
  }

  def &&(_where: Criteria): Query = {
    where(_where)
  }

  def ||(_where: Criteria): Query = {
    val the_where = if (this._where.isEmpty) None else Some(this._where.get || _where)

    this.copy(_where = the_where)
  }

  // enable: query1.where(query2)
  implicit def queryToCriteria(query: Query): Criteria = {
    query._where.getOrElse(new RawCriteria("1 = 1"))
  }

  def toSQL: String = {
    val sql = List[Option[String]]((if (_select.isEmpty) Some("select *") else _select),
      Some("from " + _from),
      _join,
      (if (_where.isEmpty) None else Some("where " + _where.get.toSQL)),
      (if (_order.isEmpty) None else Some("order by " + _order.get.toSQL)),
      _group,
      _having,
      (if (_limit.isEmpty) None else Some("limit " + _limit.get)),
      (if (_offset.isEmpty) None else Some("offset " + _offset))).filter(part => !part.isEmpty).map {
      part => part.get
    }.mkString("\n")

    sql
  }

  def executeQuery(callback: ResultSet => Unit): Unit = {
    Repository.default.within {
      conn =>
        var st: PreparedStatement = null
        var rs: ResultSet = null
        try {
          val sql = toSQL
          persistence.logger.debug(sql)
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

  override def toString = toSQL
}


//--------- Update -------
trait ModifyBase {
  def toSQL: String

  def executeUpdate = {
    val sql = toSQL
    persistence.logger.debug(sql)
    var result = false
    Repository.default.within {
      conn =>
        var st: PreparedStatement = null
        try {
          st = conn.prepareStatement(sql)
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
}

case class Update(val from: String, val sets: String, val criteria: Option[Criteria]) extends ModifyBase {
  override def toSQL = {
    ("update " + from + " set " + sets) + (if (criteria.isEmpty) "" else (" where " + criteria.get.toSQL))
  }
}

case class Delete(val from: String, val criteria: Option[Criteria]) extends ModifyBase {
  override def toSQL = {
    "delete from " + from + (if (criteria.isEmpty) "" else (" where " + criteria.get.toSQL))
  }
}

case class Insert(val from: String, val fields: String, val values: String) extends ModifyBase {
  override def toSQL = {
    "insert into " + from + "(" + fields + ") values(" + values + ")"
  }
}

//------ repository
class Repository(val name: String, val setting: Map[String, String]) {
  //TODO:hard code to mysql for now.
  def getConnection: Connection = {
    Class.forName("com.mysql.jdbc.Driver").newInstance
    val conn = DriverManager.getConnection("jdbc:mysql:///" + setting("database"),
      setting("user"), setting("password"))

    conn
  }

  def within(callback: (Connection) => Unit) {
    val conn = getConnection
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

object Repository {
  var repositories = Map[String, Repository]()

  def default = repositories("default")

  def setup(name: String, setting: Map[String, String]): Repository = {
    val repository = new Repository(name, setting)
    repositories += (name -> repository)
    repository
  }
}






