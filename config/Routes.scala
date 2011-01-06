import soupy.Routes

object Routes extends Routes{
  get("/users", Map("controller" -> "Users", "action" -> "index"))
}