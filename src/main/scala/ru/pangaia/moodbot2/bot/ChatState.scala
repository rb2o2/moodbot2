package ru.pangaia.moodbot2.bot
trait ChatState:
  val state: collection.mutable.Map[(Long, String), ConversationState] =
    new collection.mutable.HashMap[(Long, String), ConversationState]()

  def dropState(userId: String, chatId: Long): Unit =
    state --= Seq((chatId, userId))
