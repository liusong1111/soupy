package demo

import controllers.UsersController
import soupy.Routes

object MyRoutes extends Routes{
  get("/users", by[UsersController]("index"))
  get("/users/1", by[UsersController]("show"))
}