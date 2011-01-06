package controllers

import soupy.{View, Controller}

class UsersController extends Controller {
  def index {
    val title = "subject"
    val users = List("sliu", "xliu")

    object IndexView extends View("index") {
      find(".title").html(title).attr("style", "color:#ffffff")
//      find(".activity").loop(users){(elem, user) =>
//          find(".name").text(user)
//      }
    }

    render(IndexView, Map("layout" -> "users_layout"))
  }
}