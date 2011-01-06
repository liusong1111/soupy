package controllers

import soupy.{View, Controller}


object UsersController extends Controller {
  def index = {
    val title = "subject"
    val users = List("sliu", "xliu")

    render(new IndexView(title, users), Map("layout" -> "/users/users_layout"))
  }

  class ListView(val users: List[String]) extends View("/users/list") {
    override
    def render = {
      select(".users ul li").loop(users){ (context, user) =>
        context.text(user)
      }
    }
  }

  class IndexView(val title: String, val users: List[String]) extends View("/users/index") {
    override
    def render = {
      select(".title").html(title).attr("style", "color:#ffffff")
      val v = new ListView(users)
      select(".users").renderWidget(v)
    }
  }
}

