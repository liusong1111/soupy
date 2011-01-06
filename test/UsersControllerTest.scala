import controllers._
import models._
import soupy.Query
import soupy.Dao



object UsersControllerTest{
  def main(args: Array[String]) {
    println(UsersController.index)

    val m = new Query
    println(m.where("name" -> "liusong", "age" -> 32).from("users").toSQL)

    //create table users(id bigint primary key auto_increment, name varchar(20), age integer);
    //insert into users(name, age) values('liusong', 32);
    //insert into users(name, age) values('xiaoqiang', 21);
    object UserDao extends Dao(classOf[User], "users"){
      def byName(name:String)={
        q.where("name" -> "liusong")
      }
    }
    println("--all--")
    UserDao.all.foreach{user =>
      println(user.name + "^" + user.age)
    }
    println("--byName--")
    UserDao.byName("liusong").fetch.foreach(user =>
      println(user.name + "^" + user.age)
    )
    println("--Add one--")
    val user = new User()
    user.name = "xyz"
    user.age = 12
    UserDao.insert(user)
  }
}