package trvial

//----------- SQL Related -------------
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

abstract class RawCriteria extends SimpleCriteria

abstract class ListRawCriteria(val sqlTemplate: String, val args: List[Any]) extends SimpleCriteria {

}

abstract class MapRawCriteria(val sqlTemplate: String, val args: Map[Any, Any]) extends SimpleCriteria {

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

  def >(value: A) = new NormalCriteria(this, ">", value)

  def >=(value: A) = new NormalCriteria(this, ">=", value)

  def <(value: A) = new NormalCriteria(this, "<", value)

  def <=(value: A) = new NormalCriteria(this, "<=", value)

  def ==(value: A) = new NormalCriteria(this, "=", value)

  def !=(value: A) = new NormalCriteria(this, "<>", value)

  def isNull = new IsNullCriteria(this)

  def isNotNull = new IsNotNullCriteria(this)

  def in(values: List[A]) = new InCriteria(this, values)
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
  override
  def encode(value: String) = {
    //TODO: still unfinished
    "'" + value + "'"
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



