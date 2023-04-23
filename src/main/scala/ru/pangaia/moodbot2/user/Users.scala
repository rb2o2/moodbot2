package ru.pangaia.moodbot2.user

import com.pengrad.telegrambot.model.Message

trait Admin {
  def getFor(user: User): List[Message]
  def addRole(user: User, roles: Set[Role]): Unit = {
    user.roles = user.roles++roles
  }
}

trait User {
  var roles: Set[Role]
  def send(text: Message): Unit
  val id : String
}

enum Role {
  case User
  case Admin
}
