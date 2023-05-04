package ru.pangaia.moodbot2.bot

import ru.pangaia.moodbot2.data.Persistence

trait Persisting(config: Conf):
  val db = new Persistence(config)

