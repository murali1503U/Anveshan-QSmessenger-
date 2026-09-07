package com.example.meshchat.scripting

enum class ScriptType {
    LUA, JAVA
}

interface ScriptEngine {
    fun compile(source: String)
    fun process(message: String): String
}
