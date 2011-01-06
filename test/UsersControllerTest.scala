import controllers._
import reflect.BeanProperty
import soupy.Query
import soupy.Dao

class User{
  @BeanProperty
  var name:String = _

  @BeanProperty
  var age:Int = _
}

object UsersControllerTest{
  def main(args: Array[String]) {
    println(UsersController.index)

    val m = new Query
    println(m.where("name" -> "liusong", "age" -> 32).from("users").toSQL)

    //create table users(id bigint primary key auto_increment, name varchar(20), age integer);
    //insert into users(name, age) values('liusong', 32);
    object UserDao extends Dao(classOf[User], "users")
    val users = UserDao.all
    users.foreach{user =>
      println(user.name + "-" + user.age)
    }
  }
}