package trvial

//value or property initializer
trait TypeBuilder[T]{
  def apply():T
}

//base class for property
class Property[A]{

}

//for String
class StringProperty extends Property[String]
object StringProperty extends TypeBuilder[StringProperty]{
  def apply():StringProperty={
    new StringProperty()
  }
}
object StringValue extends TypeBuilder[String]{
  def apply():String={
    ""
  }
}

//for Int
class IntProperty extends Property[Int]
object IntProperty extends TypeBuilder[IntProperty]{
  def apply():IntProperty={
    new IntProperty()
  }
}
object IntValue extends TypeBuilder[Int]{
  def apply():Int={
    0
  }
}

//type definitions
trait VTypes{
  type StringType
  type IntType

  val StringType:TypeBuilder[StringType]
  val IntType:TypeBuilder[IntType]

  def field[T](builder:TypeBuilder[T]):T = {
    builder()
  }
}

trait PropertyTypes extends VTypes{
  type StringType = StringProperty
  val  StringType:TypeBuilder[StringProperty] = StringProperty

  type IntType = IntProperty
  val  IntType:TypeBuilder[IntProperty] = IntProperty

}

trait ValueTypes extends VTypes{
  type StringType = String
  val  StringType:TypeBuilder[String] = StringValue
  type IntType = Int
  val  IntType:TypeBuilder[Int] = IntValue
}

//-------------------

trait UserDef extends VTypes{
  var name = field(StringType)
  var age = field(IntType)
}

object User extends PropertyTypes with UserDef{

}

class User extends ValueTypes with UserDef{

}

object Main{
  def main(args: Array[String]) {
    val user = new User()
    user.name = "sliu"
    user.age = 32
    println(user.name + "^" + user.age)

    println(User.name)
    println(User.age)
  }
}



