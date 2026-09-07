package com.example.meshchat

import com.example.meshchat.ai.QsvmClassifier
import org.junit.Test

class DebugQsvmTest {
    @Test
    fun debug() {
        val qsvm = QsvmClassifier()
        
        fun printRes(msg: String) {
            val res = qsvm.classify(msg)
            println("MSG: '$msg' -> LEVEL: ${res.decision.level}")
            println("   SENSITIVE: ${res.decision.sensitiveFactors}")
            println("   FEATURES: ${res.extractedFeatures.toList().map { it / Math.PI }}")
            println("   SCORES: ${res.classScores}")
        }
        
        printRes("I watched a cool sci-fi movie about quantum physics.")
        printRes("Rendezvous: 48°52'3\"N 2°19'59\"E")
        printRes("My new p@ssw0rd is admin123")
    }
}
