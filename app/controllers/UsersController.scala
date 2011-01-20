package controllers

import soupy.{View, Controller}
import models._

object UsersController extends Controller {
  def index = {
    val title = "subject"
    val users = User.all

    render(new IndexView(title, users), Map("layout" -> "/users/users_layout"))
  }

  class IndexView(val title: String, val users: List[User]) extends View("/users/index") {
    override
    def render = {
      select(".title").html(title).attr("style", "color:#FF0000")
      val v = new ListView(users)
      select(".users").renderWidget(v)
    }
  }

  class ListView(val users: List[User]) extends View("/users/list") {
    override
    def render = {
      select(".users ul li").loop(users) {
        (li, user) =>
          li.select("span.name").html(user.name)
          li.html(user.name + ":" + user.age + "岁")
      }
    }
  }


}

