package models

import soupy.persistence._
import reflect.BeanInfo
import util.Random

trait UserDef extends TableDef {
  var id = field(IntType, "id")
  var name = field(StringType, "name")
  var age = field(IntType, "age")
}

@BeanInfo
class User extends Model with UserDef {

}

class UserSchema extends Schema[User, UserSchema]("users") with UserDef {
  val idProperty = IdProperty(id, "autoIncrement")

  def byName(name: String) = where(User.name == name)

  def youngs = {
    where(age < 18)
  }

  def liu = {
    where(name like "%liu%")
  }
}

object User extends UserSchema

object M1{
  def main(args: Array[String]) {
    Repository.setup("default", Map("adapter" -> "mysql", "host" -> "localhost", "database" -> "soupy", "user" -> "root", "password" -> ""))
    var u = new User
    u.name = "ttaa"
    User.insert(u)
    println(u.id)

    u.age = 45
    User.update(u)

    User.delete(u)

//    User.delete(u)
  }
}
object Main {
  def main(args: Array[String]) {
    //setup real DB connection
    Repository.setup("default", Map("adapter" -> "mysql", "host" -> "localhost", "database" -> "soupy", "user" -> "root", "password" -> ""))

    val rand = new Random
    // count before create
    println("-- count before create:" + User.count)

    //Create
    var user = new User()
    user.name = "sliu"
    user.age = rand.nextInt(70)
    User.insert(user)

    // count after create
    println("-- count after create:" + User.count)

    //Update
    user.age = rand.nextInt(70)
    User.update(user)

    //Delete
    //User.delete(user)

    //Select
    println("-- normal query --")
    var users = User.where(User.name like "liu%").where(User.age > 18).all
    users.foreach {
      user =>
        println("name:" + user.name + " age:" + user.age)
    }

    println("-- [COOL] use DAO's selector chain --")
    users = User.youngs.liu.where(User.age > 10).limit(2).all
    users.foreach {
      user =>
        println("name:" + user.name + " age:" + user.age)
    }

    println("-- first --")
    var user1 = User.where(User.id == 1).first
    if (user1.isEmpty) {
      println("not found")
    } else {
      println(user1.get)
    }

    //More Details on Query -------

    // where clause
    // age > 33
    println(User.age > 33)
    // age = 33
    println(User.age == 33)

    // name > 'liusong'
    println(User.name > "liusong")
    // name = 'liusong'
    println(User.name == "liusong")
    // name like 'liu%'
    //NOTE: only StringProperty has `like` operator. if you use it for other type, compiler will report an error.
    println(User.name like "liu%")

    //name = 'liusong' AND age > 28
    println((User.name == "liusong").where(User.age > 28))
    // same as above
    println(User.name == "liusong" && User.age > 28)

    // composite `AND` `OR`
    //(name = 'liusong' AND age > 28) OR age < 10 OR name like 'liu%'
    println(User.name == "liusong" && User.age > 28 || User.age < 10 || (User.name like "liu%"))
    //(name = 'liusong' AND (age > 28 OR age < 10)) OR name like 'liu%'
    println(User.name == "liusong" && (User.age > 28 || User.age < 10) || (User.name like "liu%"))
    // event with other complex parts
    println(User.where(User.age > 28).where(User.name like "liu%").order(User.name.desc).group("group by name"))

    //--------- internal --------------
    // look into properties
    println("-- look into properties --")
    User.properties.foreach {
      prop =>
        println(prop.name)
    }

    // use singleton object's build
    // same as `new User`
    println("-- singleton's `build` --")
    val u = User.build
    println(u.name + u.age)

    //you can use reflect style but type safe like this: User.name.set(m, "sliu")
    println("-- set/get property using type safe reflection --")
    User.name.set(user, "another name")
    println(User.name.get(user))

    println("-- use Query object directly --")
    //select *
    //from users
    //where name = 'sliu' AND age > 30
    //group by age
    println(new Query("users").where(User.name == "sliu").where(User.age > 30).group("group by age").order(User.age.desc))
  }
}
