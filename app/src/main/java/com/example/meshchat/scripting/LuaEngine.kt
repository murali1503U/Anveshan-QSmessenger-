package com.example.meshchat.scripting

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform

class LuaEngine : ScriptEngine {
    private val globals = JsePlatform.standardGlobals()
    private var luaCode: String = ""

    override fun compile(source: String) {
        luaCode = """
            $source
            if type(processMessage) == "function" then
                return processMessage(...)
            else
                return (function(msg) $source end)(...)
            end
        """.trimIndent()
        // Pre-parse to catch syntax errors early
        globals.load(luaCode)
    }

    override fun process(message: String): String {
        return try {
            val result = globals.load(luaCode).call(LuaValue.valueOf(message))
            if (result.isnil()) message else result.tojstring()
        } catch (e: Exception) {
            e.printStackTrace()
            message
        }
    }
}
