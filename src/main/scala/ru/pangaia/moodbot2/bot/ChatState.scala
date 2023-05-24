package ru.pangaia.moodbot2.bot
trait ChatState:
  type Context = List[Any]
  type State = collection.mutable.Map[(Long, String), (ConversationState, Context)]
  val state: State =
    new collection.mutable.HashMap[(Long, String), (ConversationState, Context)]()

  def dropState(chatId: Long, userId: String): Unit =
    state --= Seq((chatId, userId))
