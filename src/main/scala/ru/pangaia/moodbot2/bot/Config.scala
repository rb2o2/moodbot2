package ru.pangaia.moodbot2.bot

import java.nio.charset.StandardCharsets
import scala.io.Source

object Config {
  val getToken: String = {
    val stream = getClass.getClassLoader.getResourceAsStream("token")
    val res = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
    stream.close()
    res
  }
  val maxBatches = 4
}
