package ru.pangaia.moodbot2.bot

import java.nio.charset.StandardCharsets
import java.util.{Properties, PropertyResourceBundle, ResourceBundle}
import scala.io.Source

object Config:
  private val token: String =
    val stream = getClass.getClassLoader.getResourceAsStream("token")
    val res = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
    stream.close()
    res
  
  def apply(profile: String): Conf =
    (key: String) => key match
      case "token" => token
      case _ => prop(profile)(key)
  
  private def prop: String => String => String =
    profile =>
      key =>
        val p = if profile.isEmpty then "" else "-" + profile
        val stream = this
          .getClass.getClassLoader
          .getResourceAsStream(s"application$p.conf")
        val resourceBundle = new PropertyResourceBundle(stream)
        stream.close()
          resourceBundle.getString(key)
