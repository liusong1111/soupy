import controllers._
import models._
import soupy.Query
import soupy.Dao



object UsersControllerTest{
  def main(args: Array[String]) {
    soupy.application = MyApplication

    println(UsersController.index)

    val m = new Query
    println(m.where("name" -> "liusong", "age" -> 32).from("users").toSQL)

    //create table users(id bigint primary key auto_increment, name varchar(20), age integer);
    //insert into users(name, age) values('liusong', 32);
    //insert into users(name, age) values('xiaoqiang', 21);
    println("--all--")
    User.all.foreach{user =>
      println(user.name + "^" + user.age)
    }
    println("--byName--")
    User.byName("liusong").fetch.foreach(user =>
      println(user.name + "^" + user.age)
    )
    println("--Add one--")
    val user = new User()
    user.name = "xyz"
    user.age = 12
    println("before count:" + User.count)
    User.insert(user)
    println("after count:" + User.count)
  }
}