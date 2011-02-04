package demo

import controllers._
import soupy.Routes

object MyRoutes extends Routes{
  get("/users", by[UsersController]("index"))
  get("/users/1", by[UsersController]("show"))
  get("/search", by[SearchPortalController]("index"))

  get("/admin/manage_search", by[SearchManageController]("index"))
}