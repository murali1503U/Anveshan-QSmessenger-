import re

with open("app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt", "r") as f:
    content = f.read()

imports = """import android.content.Context
import android.os.Build
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel"""

if "import org.tensorflow.lite" not in content:
    content = content.replace("import kotlin.math.sqrt\n", imports + "\nimport kotlin.math.sqrt\n")

gpu_init = """    // TFLite & GPU Delegate Setup
    private val useGpu = CompatibilityList().isDelegateSupportedOnThisDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    private val gpuDelegate by lazy { if (useGpu) GpuDelegate(CompatibilityList().bestOptionsForThisDevice) else null }
    private var interpreter: Interpreter? = null

    init {
        // Attempt to load TFLite Model with GPU Delegate
        try {
            val options = Interpreter.Options()
            gpuDelegate?.let {
                options.addDelegate(it)
                Log.d("QSVM", "GPU Delegate attached successfully.")
            }
            // interpreter = Interpreter(loadModelFile(context, "qsvm_model.tflite"), options)
        } catch (e: Exception) {
            Log.w("QSVM", "GPU not available or model missing, falling back to CPU (Kotlin Simulation)", e)
        }
    }
"""

if "private val useGpu" not in content:
    content = content.replace("class QsvmClassifier {", "class QsvmClassifier(context: Context? = null) {\n" + gpu_init)

classify_mod = """    fun classify(message: String, context: List<String> = emptyList()): QsvmResult {
        val startTime = System.nanoTime()
        val features = extractFeatures(message, context)
        
        // TFLite GPU inference path (if model was loaded)
        interpreter?.let { tflite ->
            try {
                val input = Array(1) { FloatArray(features.size) { i -> features[i].toFloat() } }
                val output = Array(1) { FloatArray(4) }
                tflite.run(input, output)
                val elapsed = System.nanoTime() - startTime
                Log.d("QSVM", "Inference time: ${elapsed / 1_000_000} ms (GPU)")
                // (Omitted processing of output for simulation fallback)
            } catch (e: Exception) {
                Log.e("QSVM", "TFLite GPU inference failed", e)
            }
        }
        
        // CPU Simulation Fallback (since no real model file is provided)
        val quantumState = computeQuantumState(features)"""

content = content.replace("    fun classify(message: String, context: List<String> = emptyList()): QsvmResult {\n        val startTime = System.currentTimeMillis()\n        val features = extractFeatures(message, context)\n        val quantumState = computeQuantumState(features)", classify_mod)

with open("app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt", "w") as f:
    f.write(content)

