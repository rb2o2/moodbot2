package ru.pangaia.moodbot2.bot

import java.nio.charset.StandardCharsets
import java.util.Properties
import scala.io.Source

object Config:
  val token: String =
    val stream = getClass.getClassLoader.getResourceAsStream("token")
    val res = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
    stream.close()
    res
  
  def apply(profile: String): Conf =
    (key: String) => prop(profile)(key)
  
  private def prop: String => String => String =
    profile =>
      key =>
        val props =
          val p = if profile.isEmpty then "" else "-" + profile
          val stream = this
            .getClass.getClassLoader
            .getResourceAsStream(s"application$p.properties")
          new Properties():
            load(stream)
            stream.close()
        props.getProperty(key)
