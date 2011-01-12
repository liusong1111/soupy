package trial

import java.sql.{DriverManager, Connection, ResultSet, PreparedStatement}
import java.beans.Introspector
import java.beans.PropertyDescriptor
import reflect.BeanInfo

//------------dirty Logger--------
object Logger {
  def debug(msg: String) = {
    println("--debug--:" + msg)
  }

  def info(msg: String) = {
    println("--info--:" + msg)
  }

  def error(msg: String) = {
    println("--error--:" + msg)
  }
}

//----------- SQL Related -------------
//--------Order --------------
trait Order {
  def toSQL: String

  override def toString = toSQL
}

trait SimpleOrder extends Order

class DescOrder[A](val prop: Property[A]) extends SimpleOrder {
  override def toSQL = prop.name + " DESC"
}

class AscOrder[A](val prop: Property[A]) extends SimpleOrder {
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

trait PropertyTypeBuilder[T] extends TypeBuilder[T] {
  type M
  val modelClass: Class[M]
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

trait StringPropertyBuilder extends PropertyTypeBuilder[StringProperty] {
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

trait IntPropertyBuilder extends PropertyTypeBuilder[IntProperty] {
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
  type M
  val modelClass: Class[M]

  type StringType
  type IntType

  val StringType: TypeBuilder[StringType]
  val IntType: TypeBuilder[IntType]

  def field[T](builder: TypeBuilder[T], name: String, options: Pair[String, String]*): T
}

trait PropertiesDef extends TableDef {
  self =>
  type StringType = StringProperty

  lazy val StringType: TypeBuilder[StringProperty] = new StringPropertyBuilder {
    type M = self.M
    val modelClass: Class[M] = self.modelClass
  }

  type IntType = IntProperty
  lazy val IntType: TypeBuilder[IntProperty] = new IntPropertyBuilder {
    type M = self.M
    val modelClass = self.modelClass
  }

  var properties = List[Property[Any]]()

  override def field[T](builder: TypeBuilder[T], name: String, options: Pair[String, String]*): T = {
    val index = (properties.length + 1)
    val prop = builder(name, index)
    properties = properties ::: List(prop.asInstanceOf[Property[Any]])
    prop
  }
}

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

trait Copyable {
  self: {def copy(query: Query): Any} =>

  type DAO <: Schema

  def dup(query: Query): DAO = {
    val ins = self.copy(query)
    ins.asInstanceOf[DAO]
  }
}

trait QueryDelegator extends Copyable {
  self: Schema {def copy(query: Query): Any} =>

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
  def order(_order: CompositeOrder): DAO = {
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

trait Schema extends PropertiesDef with QueryDelegator {
  self: {def copy(query: Query): Any} =>

  type DAO <: Schema

  val query: Query

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

}

trait Model extends AccessorsDef

//------ Query --------------------
case class Query(val _from: String = null,
                 val _select: Option[String] = None,
                 val _join: Option[String] = None,
                 val _where: Option[Criteria] = None,
                 val _order: Option[CompositeOrder] = None,
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
  def order(_order: CompositeOrder): Query = {
    val the_order = if (this._order.isEmpty) _order else new CompositeOrder(List[SimpleOrder]((this._order.get.orders ::: _order.orders): _*))
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
          Logger.debug(sql)
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
    ("update " + from + " " + sets) + (if (criteria.isEmpty) "" else (" where " + criteria.get.toSQL))
  }
}

case class Delete(val from: String, val criteria: Option[Criteria]) extends ModifyBase {
  override def toSQL = {
    "delete " + from + (if (criteria.isEmpty) "" else (" where " + criteria.get.toSQL))
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


//------------------------------
//---- here is the demo---------
//------------------------------

trait UserDef extends TableDef {
  //still a little verbose.
  type M = User
  val modelClass = classOf[User]


  var name = field(StringType, "name")
  var age = field(IntType, "age")
}

@BeanInfo
class User extends Model with UserDef {

}

//don't specify vars in the case class.
case class UserSchema(override val query: Query) extends Schema with UserDef {
  type DAO = UserSchema

  def youngs = {
    where(age < 18)
  }

  def liu = {
    where(name like "%liu%")
  }
}

// NOTE: don't write things in object's body.
// because we use case class's copy method to generate immutable objects, representing internal query.
// cast to object User will cause ClassCastException.
// sorry for that.
// any better solution?
object User extends UserSchema(Query("users"))


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
    // name desc
    println(User.name.desc)

    User.properties.foreach {
      prop =>
        println(prop.name)
    }

    val u = User.build
    println("^^^^")
    println(u.name + u.age)

    //you can use reflect style but type safe like this: User.name.set(m, "sliu")
    User.name.set(user, "another name")
    println(User.name.get(user))

    //select *
    //from users
    //where name = 'sliu' AND age > 30
    //group by age
    println(new Query("users").where(User.name == "sliu").where(User.age > 30).group("group by age")) //.order(User.age.desc)

    //select *
    //from users
    //where age < 18 AND name like '%liu%' AND age > 10
    //limit 2
    println(User.youngs.liu.where(User.age > 10).limit(2))

    //setup real DB connection
    Repository.setup("default", Map("adapter" -> "mysql", "host" -> "localhost", "database" -> "soupy", "user" -> "root", "password" -> ""))

    //iterate the models
    val youngs = User.youngs
    youngs.all.foreach {
      user =>
        println("name:" + user.name + " age:" + user.age)
    }

    //check one
    val liu = youngs.where(User.name like "%liu").first
    if (liu.isEmpty) {
      println("not found")
    } else {
      println("name:" + liu.get.name + " age:" + liu.get.age)
    }
  }
}



