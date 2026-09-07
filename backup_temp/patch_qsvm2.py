import re

with open("app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt", "r") as f:
    content = f.read()

# applyHadamardAll
target1 = """    private fun applyHadamardAll(state: Array<Complex>) {
        val invSqrt2 = 1.0 / sqrt(2.0)
        for (q in 0 until numQubits) {
            val bit = 1 shl q
            for (i in 0 until stateDim) {
                if ((i and bit) == 0) {
                    val j = i or bit
                    val u = state[i]
                    val v = state[j]
                    state[i] = (u + v) * invSqrt2
                    state[j] = (u - v) * invSqrt2
                }
            }
        }
    }"""
repl1 = """    private fun applyHadamardAll(state: Array<Complex>) {
        val invSqrt2 = 1.0 / sqrt(2.0)
        (0 until numQubits).forEach { q ->
            val bit = 1 shl q
            (0 until stateDim).filter { (it and bit) == 0 }.forEach { i ->
                val j = i or bit
                val u = state[i]
                val v = state[j]
                state[i] = (u + v) * invSqrt2
                state[j] = (u - v) * invSqrt2
            }
        }
    }"""
content = content.replace(target1, repl1)

# applyZRotation
target2 = """    private fun applyZRotation(state: Array<Complex>, qubit: Int, theta: Double) {
        val cosHalf = cos(theta / 2.0)
        val sinHalf = sin(theta / 2.0)
        val expMinus = Complex(cosHalf, -sinHalf)
        val expPlus = Complex(cosHalf, sinHalf)
        val bit = 1 shl qubit
        for (i in 0 until stateDim) {
            if ((i and bit) == 0) {
                state[i] = state[i] * expMinus
            } else {
                state[i] = state[i] * expPlus
            }
        }
    }"""
repl2 = """    private fun applyZRotation(state: Array<Complex>, qubit: Int, theta: Double) {
        val cosHalf = cos(theta / 2.0)
        val sinHalf = sin(theta / 2.0)
        val expMinus = Complex(cosHalf, -sinHalf)
        val expPlus = Complex(cosHalf, sinHalf)
        val bit = 1 shl qubit
        (0 until stateDim).forEach { i ->
            state[i] = state[i] * if ((i and bit) == 0) expMinus else expPlus
        }
    }"""
content = content.replace(target2, repl2)

# applyZZRotation
target3 = """    private fun applyZZRotation(state: Array<Complex>, qubit1: Int, qubit2: Int, phi: Double) {
        val cosHalf = cos(phi / 2.0)
        val sinHalf = sin(phi / 2.0)
        val expMinus = Complex(cosHalf, -sinHalf)
        val expPlus = Complex(cosHalf, sinHalf)
        val bit1 = 1 shl qubit1
        val bit2 = 1 shl qubit2
        for (i in 0 until stateDim) {
            val b1 = (i and bit1) != 0
            val b2 = (i and bit2) != 0
            // If parity b1 XOR b2 is 0 (eigenvalue +1), phase is exp(+i*phi/2), else exp(-i*phi/2)
            if (b1 == b2) {
                state[i] = state[i] * expPlus
            } else {
                state[i] = state[i] * expMinus
            }
        }
    }"""
repl3 = """    private fun applyZZRotation(state: Array<Complex>, qubit1: Int, qubit2: Int, phi: Double) {
        val cosHalf = cos(phi / 2.0)
        val sinHalf = sin(phi / 2.0)
        val expMinus = Complex(cosHalf, -sinHalf)
        val expPlus = Complex(cosHalf, sinHalf)
        val bit1 = 1 shl qubit1
        val bit2 = 1 shl qubit2
        (0 until stateDim).forEach { i ->
            state[i] = state[i] * if (((i and bit1) != 0) == ((i and bit2) != 0)) expPlus else expMinus
        }
    }"""
content = content.replace(target3, repl3)

with open("app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt", "w") as f:
    f.write(content)

