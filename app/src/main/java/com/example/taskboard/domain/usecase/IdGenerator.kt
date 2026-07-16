package com.example.taskboard.domain.usecase

/** Supplies identities for newly created tasks. Injected so it can be faked in tests. */
fun interface IdGenerator {
    fun next(): String
}
