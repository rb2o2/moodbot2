package ru.pangaia.moodbot2.bot

import org.scalatest.flatspec.AnyFlatSpec

class ConfigTest extends AnyFlatSpec:
  val config: Conf = Config()

  "getToken" should "read resource file properly" in 
    assert(config.token === "test1234")
  
