package ru.pangaia.moodbot2.data

import java.sql.Timestamp
import java.util.UUID

case class Message(id: UUID, userId: UUID, messageText: String, creationTimestamp: Timestamp, reportedTime: Timestamp)
case class User(id: UUID, username: String)
case class Mark(id: UUID, userId: UUID, markValue: Int, creationTimestamp: Timestamp)
