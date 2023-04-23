package ru.pangaia.moodbot2.bot

import org.scalatest.flatspec.AnyFlatSpec

class ConfigTest extends AnyFlatSpec {

  "getToken" should "read resource file properly" in {
    assert(Config.getToken === "test1234")
  }


}
