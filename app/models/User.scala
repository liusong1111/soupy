package models

import reflect.BeanProperty
import soupy.Dao

class User {
  @BeanProperty
  var name: String = _

  @BeanProperty
  var age: Int = _
}

object User extends Dao(classOf[User], "users") {
  def byName(name: String) = {
    q.where("name" -> "liusong")
  }
}