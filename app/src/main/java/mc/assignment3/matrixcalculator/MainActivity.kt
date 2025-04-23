package mc.assignment3.matrixcalculator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import mc.assignment3.matrixcalculator.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var matrixAFields: List<List<EditText>>
    private lateinit var matrixBFields: List<List<EditText>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Generate input fields when button is clicked
        binding.generateMatricesBtn.setOnClickListener {
            val rows = binding.rowsInput.text.toString().toIntOrNull()
            val cols = binding.colsInput.text.toString().toIntOrNull()

            if (rows == null || cols == null || rows <= 0 || cols <= 0) {
                Toast.makeText(this, "Please enter valid dimensions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            generateMatrixInputs(rows, cols)
        }

        binding.openWifiLoggerBtn.setOnClickListener {
            startActivity(Intent(this, WifiLoggingActivity::class.java))
        }

    }

    private fun generateMatrixInputs(rows: Int, cols: Int) {
        binding.matrixAContainer.removeAllViews()
        binding.matrixBContainer.removeAllViews()

        matrixAFields = List(rows) { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val rowFields = List(cols) {
                EditText(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    hint = "0"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                }.also { rowLayout.addView(it) }
            }
            binding.matrixAContainer.addView(rowLayout)
            rowFields
        }

        matrixBFields = List(rows) { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val rowFields = List(cols) {
                EditText(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    hint = "0"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                }.also { rowLayout.addView(it) }
            }
            binding.matrixBContainer.addView(rowLayout)
            rowFields
        }

        binding.addBtn.setOnClickListener {
            performMatrixOperation(rows, cols, true)
        }

        binding.subBtn.setOnClickListener {
            performMatrixOperation(rows, cols, false)
        }
    }

    private fun performMatrixOperation(rows: Int, cols: Int, add: Boolean) {
        val matA = Array(rows) { r ->
            FloatArray(cols) { c ->
                matrixAFields[r][c].text.toString().toFloatOrNull() ?: 0f
            }
        }

        val matB = Array(rows) { r ->
            FloatArray(cols) { c ->
                matrixBFields[r][c].text.toString().toFloatOrNull() ?: 0f
            }
        }
        var result = Array(rows) { FloatArray(cols) }
        if (add) {
            result = addMatrices(matA, matB, rows, cols)
        }
        else {
            result = subMatrices(matA, matB, rows, cols)
        }

        val resultString = result.joinToString("\n") { row ->
            row.joinToString(" ") { "%.2f".format(it) }
        }

        binding.resultText.text = "Result:\n$resultString"
    }

    external fun addMatrices(matA: Array<FloatArray>, matB: Array<FloatArray>, rows: Int, cols: Int): Array<FloatArray>

    external fun subMatrices(matA: Array<FloatArray>, matB: Array<FloatArray>, rows: Int, cols: Int): Array<FloatArray>

    companion object {
        init {
            System.loadLibrary("matrixcalculator")
        }
    }


}
