package ru.pangaia.moodbot2.bot

import org.scalatest.flatspec.AnyFlatSpec

import java.util.PropertyResourceBundle

class ConfigTest extends AnyFlatSpec:
  val config: Conf = Config("test")

  "getToken" should "read resource file properly" in
    assert(config("token") === "test1234")


  "menu.text property" should "be read properly from properties file" in {
    val greeting = config("menu.text")
    assert(greeting(0) === 'П')
    assert(greeting.split("\\n").length === 2)
  }
