package com.example.meshchat.scripting

import org.codehaus.janino.SimpleCompiler

class JaninoEngine : ScriptEngine {
    private val compiler = SimpleCompiler()
    private var compiledClass: Class<*>? = null

    override fun compile(source: String) {
        try {
            // Janino generates JVM bytecode, which is natively incompatible with Android ART.
            // We implement it for completeness in the architecture, but wrap it safely.
            compiler.cook(source)
            compiledClass = compiler.classLoader.loadClass("Script")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun process(message: String): String {
        return try {
            if (compiledClass != null) {
                val instance = compiledClass!!.getDeclaredConstructor().newInstance()
                val method = compiledClass!!.getDeclaredMethod("processMessage", String::class.java)
                method.invoke(instance, message) as? String ?: message
            } else {
                message
            }
        } catch (e: Exception) {
            e.printStackTrace()
            message
        }
    }
}
