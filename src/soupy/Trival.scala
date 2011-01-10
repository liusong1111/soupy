package trvial

import java.sql.{DriverManager, Connection, ResultSet, PreparedStatement}

//----------- SQL Related -------------
//--------Order --------------
trait Order {
  def toSQL: String
}

trait SimpleOrder extends Order

class DescOrder(val prop: Property[_]) extends SimpleOrder {
  override def toSQL = prop.name + " DESC"
}

class AscOrder(val prop: Property[_]) extends SimpleOrder {
  override def toSQL = prop.name + " ASC"
}

class CompositeOrder(val orders: List[SimpleOrder]) extends Order {
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

class IsNullCriteria(val prop: Property[_]) extends SimpleCriteria {
  override def toSQL = prop.name + " is null"
}

class IsNotNullCriteria(val prop: Property[_]) extends SimpleCriteria {
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

//value or property initializer
trait TypeBuilder[T] {
  def apply(name: String): T
}

//base class for property

abstract class Property[A](val name: String) extends PropertyOperations[A] {
  def encode(value: A): String = {
    value.toString
  }
}

//for String
class StringProperty(override val name: String) extends Property[String](name) {
  val singleQuoteRegexp = """'""".r

  override
  def encode(value: String) = {
    "'" + singleQuoteRegexp.replaceAllIn(value, "''") + "'"
  }

  def like(value: String) = {
    new LikeCriteria(this, value)
  }
}

object StringProperty extends TypeBuilder[StringProperty] {
  def apply(name: String): StringProperty = {
    new StringProperty(name)
  }
}

object StringValue extends TypeBuilder[String] {
  def apply(name: String): String = {
    ""
  }
}

//for Int
class IntProperty(override val name: String) extends Property[Int](name: String)

object IntProperty extends TypeBuilder[IntProperty] {
  def apply(name: String): IntProperty = {
    new IntProperty(name)
  }
}

object IntValue extends TypeBuilder[Int] {
  def apply(name: String): Int = {
    0
  }
}

//type definitions
trait TableDef {
  type StringType
  type IntType

  val StringType: TypeBuilder[StringType]
  val IntType: TypeBuilder[IntType]

  def field[T](builder: TypeBuilder[T], name: String, options: Pair[String, String]*): T = {
    builder(name)
  }
}

trait PropertiesDef extends TableDef {
  type StringType = StringProperty
  val StringType: TypeBuilder[StringProperty] = StringProperty

  type IntType = IntProperty
  val IntType: TypeBuilder[IntProperty] = IntProperty

}

trait AccessorsDef extends TableDef {
  type StringType = String
  val StringType: TypeBuilder[String] = StringValue
  type IntType = Int
  val IntType: TypeBuilder[Int] = IntValue
}

trait Schema extends PropertiesDef

trait Model extends AccessorsDef

//------ Query --------------------
class Query(val _from: String,
            val _select: Option[String] = None,
            val _join: Option[String] = None,
            val _where: Option[Criteria] = None,
            val _order: Option[CompositeOrder] = None,
            val _group: Option[String] = None,
            val _having: Option[String] = None,
            val _offset: Option[Int] = None,
            val _limit: Option[Int] = None) {
  def copy(_from: String,
           _select: Option[String],
           _join: Option[String],
           _where: Option[Criteria],
           _order: Option[CompositeOrder],
           _group: Option[String],
           _having: Option[String],
           _offset: Option[Int],
           _limit: Option[Int]): Query = {
    new Query(_from,
      _select,
      _join,
      _where,
      _order,
      _group,
      _having,
      _offset,
      _limit)
  }

  def from(_from: String): Query = {
    this.copy(_from,
      _select,
      _join,
      _where,
      _order,
      _group,
      _having,
      _offset,
      _limit)
  }

  def select(_select: String): Query = {
    this.copy(_from,
      Some(_select),
      _join,
      _where,
      _order,
      _group,
      _having,
      _offset,
      _limit)
  }

  def join(_join: String): Query = {
    this.copy(_from,
      _select,
      Some(_join),
      _where,
      _order,
      _group,
      _having,
      _offset,
      _limit)
  }

  def group(_group: String): Query = {
    this.copy(_from,
      _select,
      _join,
      _where,
      _order,
      Some(_group),
      _having,
      _offset,
      _limit)
  }

  def having(_having: String): Query = {
    this.copy(_from,
      _select,
      _join,
      _where,
      _order,
      _group,
      Some(_having),
      _offset,
      _limit)
  }

  def offset(_offset: Int): Query = {
    this.copy(_from,
      _select,
      _join,
      _where,
      _order,
      _group,
      _having,
      Some(_offset),
      _limit)
  }

  def limit(_limit: Int): Query = {
    this.copy(_from,
      _select,
      _join,
      _where,
      _order,
      _group,
      _having,
      _offset,
      Some(_limit)
    )
  }

  // composite order
  def order(_order: CompositeOrder): Query = {
    val the_order = if (this._order.isEmpty) _order else new CompositeOrder(List[SimpleOrder]((this._order.get.orders ::: _order.orders): _*))
    this.copy(_from,
      _select,
      _join,
      _where,
      Some(the_order),
      _group,
      _having,
      _offset,
      _limit)
  }

  // composite criteria
  def where(_where: Criteria): Query = {
    val the_where = if (this._where.isEmpty) {
      _where
    } else {
      this._where.get && _where
    }

    this.copy(_from,
      _select,
      _join,
      Some(the_where),
      _order,
      _group,
      _having,
      _offset,
      _limit)
  }

  def &&(_where: Criteria): Query = {
    where(_where)
  }

  def ||(_where: Criteria): Query = {
    val the_where = if (this._where.isEmpty) None else Some(this._where.get || _where)

    this.copy(_from,
      _select,
      _join,
      the_where,
      _order,
      _group,
      _having,
      _offset,
      _limit)
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
}


//--------- Update -------
trait ModifyBase{
  def toSQL:String

  def executeUpdate={
    val sql = toSQL
    var result = false
    Repository.default.within{
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
class Update(val from: String, val sets: String, val criteria: Option[Criteria]) extends ModifyBase {
  override def toSQL = {
    ("update " + from + " " + sets) + (if (criteria.isEmpty) "" else (" where " + criteria.get.toSQL))
  }
}

class Delete(val from: String, val criteria: Option[Criteria]) extends ModifyBase  {
  override def toSQL = {
    "delete " + from + (if (criteria.isEmpty) "" else (" where " + criteria.get.toSQL))
  }
}

class Insert(val from: String, val fields: String, val values: String) extends ModifyBase  {
  override def toSQL = {
    "insert into " + from + "(" + fields + ") values(" + values + ")"
  }
}

//------ repository
class Repository(val name: String, val setting: Map[String, String]) {
  var connection: Connection = getConnection

  //TODO:目前只支持mysql
  def getConnection: Connection = {
    Class.forName("com.mysql.jdbc.Driver").newInstance
    val conn = DriverManager.getConnection("jdbc:mysql:///" + setting("database"),
      setting("user"), setting("password"))

    conn
  }

  def within(callback: (Connection) => Unit) {
    val conn = connection
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
  val repositories = Map[String, Repository]()

  def default = repositories("default")

  def setup(name: String, setting: Map[String, String]): Repository = {
    val repository = new Repository(name, setting)
    repositories(name) = repository
    repository
  }


}


//------------------------------
//---- here is the demo---------
//------------------------------

trait UserDef extends TableDef {
  //still duplicate on property name.
  //note: should archive: generate var declaration and var initialization
  //Is there any approach to avoid them?
  var name = field(StringType, "name")
  var age = field(IntType, "age")
}

class User extends Model with UserDef {

}

object User extends Schema with UserDef {

}

object Main {
  def main(args: Array[String]) {
    val user = new User()
    // type safe
    user.name = "sliu"
    user.age = 32
    println(user.name + "^" + user.age)

    //trvial.StringProperty@7b6889
    println(User.name)

    // where clause
    // age>33
    println(User.age > 33)
    println(User.age == 33)

    // name>'liusong'
    println(User.name > "liusong")
    println(User.name == "liusong")

    //name='liusong' AND age>28
    println((User.name == "liusong").where(User.age > 28))
    println(User.name == "liusong" && User.age > 28)

    //NOTE: only User.name has like method, User.age can't(results compiler error).
    //println(User.age.like("%a"))   // error: value `like` is not a memeber

    //(name = 'liusong' AND age > 28) OR age < 10 OR name like 'liu%'
    println(User.name == "liusong" && User.age > 28 || User.age < 10 || (User.name like "liu%"))
    //(name = 'liusong' AND (age > 28 OR age < 10)) OR name like 'liu%'
    println(User.name == "liusong" && (User.age > 28 || User.age < 10) || (User.name like "liu%"))
  }
}



