package demo

import controllers._
import soupy.{App, Routes}

object MyRoutes extends Routes{
  get("/users", App[UsersController]("index"))
  get("/users/1", App[UsersController]("show"))
  get("/search", App[SearchPortalController]("index"))

  get("/admin/manage_search", App[SearchManageController]("index"))
}