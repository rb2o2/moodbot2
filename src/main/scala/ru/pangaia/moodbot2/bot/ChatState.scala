package ru.pangaia.moodbot2.bot
trait ChatState:
  type Context = List[Any]
  type State = collection.mutable.Map[(Long, String), (ConversationState, Context)]
  val state: State =
    new collection.mutable.HashMap[(Long, String), (ConversationState, Context)]()

  def dropState(userId: String, chatId: Long): Unit =
    state --= Seq((chatId, userId))
