package com.aistudio.fasmovapos.app.superapp

data class RecordReference(
  val table: String,
  val id: String,
  val date: String,
  val voided: Boolean,
)
