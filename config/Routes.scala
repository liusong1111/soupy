package demo

import controllers.UsersController
import soupy.Routes

object MyRoutes extends Routes{
  get("/users", Map("controller" -> UsersController, "action" -> "index"))
}