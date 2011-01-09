package trvial

//----------- SQL Related -------------
trait Criteria {
  def toSQL: String

  override def toString = toSQL

  def where(criteria: Criteria) = {
    new And(this, criteria)
  }

  def &&(criteria: Criteria) = {
    where(criteria)
  }

  def ||(criteria: Criteria) = {
    new Or(this, criteria)
  }
}

class Where[A](val prop: Property[A], val op: String, val value: A) extends Criteria {
  override def toSQL = prop.name + " " + op + " " + prop.encode(value)
}

class And(val criterias: Criteria*) extends Criteria {
  override
  def toSQL = {
    criterias.map {
      criteria => if (criteria.isInstanceOf[Or]) "(" + criteria.toSQL + ")" else criteria.toSQL
    }.mkString(" AND ")
  }
}

class Or(val criterias: Criteria*) extends Criteria {
  override
  def toSQL = {
    criterias.map {
      criteria => if (criteria.isInstanceOf[And]) "(" + criteria.toSQL + ")" else criteria.toSQL
    }.mkString(" OR ")
  }
}

trait PropertyOperations[A] {
  self: Property[A] =>

  def >(value: A) = {
    new Where(this, ">", value)
  }

  def >=(value: A) = {
    new Where(this, ">=", value)
  }

  def <(value: A) = {
    new Where(this, "<", value)
  }

  def <=(value: A) = {
    new Where(this, "<=", value)
  }

  def ==(value: A) = {
    new Where(this, "=", value)
  }

  def !=(value: A) = {
    new Where(this, "<>", value)
  }
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
    new Where(this, "like", value)
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
trait VTypes {
  type StringType
  type IntType

  val StringType: TypeBuilder[StringType]
  val IntType: TypeBuilder[IntType]

  def field[T](builder: TypeBuilder[T], name: String, options: Pair[String, String]*): T = {
    builder(name)
  }

  def f(s: String) = {
    println(s)
  }
}

trait PropertyTypes extends VTypes {
  type StringType = StringProperty
  val StringType: TypeBuilder[StringProperty] = StringProperty

  type IntType = IntProperty
  val IntType: TypeBuilder[IntProperty] = IntProperty

}

trait ValueTypes extends VTypes {
  type StringType = String
  val StringType: TypeBuilder[String] = StringValue
  type IntType = Int
  val IntType: TypeBuilder[Int] = IntValue
}

//------------------------------
//---- here is the demo---------
//------------------------------

trait UserDef extends VTypes {
  //still duplicate on property name.
  //note: should archive: generate var declaration and var initialization
  //Is there any approach to avoid them?
  var name = field(StringType, "name")
  var age = field(IntType, "age")
}

object User extends PropertyTypes with UserDef {

}

class User extends ValueTypes with UserDef {

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



