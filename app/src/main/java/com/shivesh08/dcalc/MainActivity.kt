package com.shivesh08.dcalc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var calculatorLayout: LinearLayout
    private lateinit var selectionLayout: LinearLayout
    private lateinit var display: TextView
    private var currentInput = ""
    private var firstOperand: Double? = null
    private var currentOperator: String? = null
    private var secretPin: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        calculatorLayout = findViewById(R.id.calculatorLayout)
        selectionLayout = findViewById(R.id.selectionLayout)
        display = findViewById(R.id.display)

        val prefs = getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
        secretPin = prefs.getString("secret_pin", null)
        if (secretPin == null) {
            display.text = "Set PIN and press ="
        }

        setupCalculator()
        setupWebView()
        setupSelectionButtons()
    }

    private fun setupSelectionButtons() {
        findViewById<Button>(R.id.btnDiscord).setOnClickListener {
            launchUrl("https://discord.com/app")
        }
        findViewById<Button>(R.id.btnInstagram).setOnClickListener {
            launchUrl("https://www.instagram.com")
        }
    }

    private fun setupCalculator() {
        val gridLayout = calculatorLayout.getChildAt(1) as GridLayout
        for (i in 0 until gridLayout.childCount) {
            val view = gridLayout.getChildAt(i)
            if (view is Button) {
                view.setOnClickListener { onButtonClick(view.text.toString()) }
            }
        }
    }

    private fun onButtonClick(text: String) {
        if (secretPin == null) {
            when (text) {
                "C" -> {
                    currentInput = ""
                    display.text = "Set PIN"
                }
                "DEL" -> {
                    if (currentInput.isNotEmpty()) {
                        currentInput = currentInput.substring(0, currentInput.length - 1)
                        display.text = if (currentInput.isEmpty()) "Set PIN" else currentInput
                    }
                }
                "=" -> {
                    if (currentInput.length >= 4) {
                        secretPin = currentInput
                        getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("secret_pin", secretPin)
                            .apply()
                        currentInput = ""
                        display.text = "0"
                    } else {
                        display.text = "Error"
                        currentInput = ""
                    }
                }
                "/", "*", "-", "+", "." -> {}
                else -> {
                    if (currentInput.length < 10) {
                        currentInput += text
                        display.text = currentInput
                    }
                }
            }
            return
        }

        when (text) {
            "C" -> {
                currentInput = ""
                firstOperand = null
                currentOperator = null
                display.text = "0"
            }
            "DEL" -> {
                if (currentInput.isNotEmpty()) {
                    currentInput = currentInput.substring(0, currentInput.length - 1)
                    display.text = if (currentInput.isEmpty()) "0" else currentInput
                }
            }
            "=" -> {
                if (currentInput == secretPin) {
                    showSelection()
                    return
                }
                calculate()
            }
            "/", "*", "-", "+" -> {
                if (currentInput.isNotEmpty()) {
                    firstOperand = currentInput.toDoubleOrNull()
                    currentOperator = text
                    currentInput = ""
                }
            }
            "." -> {
                if (!currentInput.contains(".")) {
                    currentInput += if (currentInput.isEmpty()) "0." else "."
                    display.text = currentInput
                }
            }
            else -> {
                if (currentInput.length < 10) {
                    currentInput += text
                    display.text = currentInput
                }
            }
        }
    }

    private fun calculate() {
        val secondOperand = currentInput.toDoubleOrNull()
        if (firstOperand != null && currentOperator != null && secondOperand != null) {
            val result = when (currentOperator) {
                "+" -> firstOperand!! + secondOperand
                "-" -> firstOperand!! - secondOperand
                "*" -> firstOperand!! * secondOperand
                "/" -> if (secondOperand != 0.0) firstOperand!! / secondOperand else Double.NaN
                else -> 0.0
            }
            val resultString = formatResult(result)
            display.text = resultString
            currentInput = resultString
            firstOperand = null
            currentOperator = null
        }
    }

    private fun formatResult(result: Double): String {
        if (result.isNaN()) return "Error"
        val longRes = result.toLong()
        return if (result == longRes.toDouble()) {
            longRes.toString()
        } else {
            val s = result.toString()
            if (s.length > 10) s.substring(0, 10) else s
        }
    }

    private fun showSelection() {
        calculatorLayout.visibility = View.GONE
        selectionLayout.visibility = View.VISIBLE
        webView.visibility = View.GONE
    }

    private fun launchUrl(url: String) {
        selectionLayout.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE || selectionLayout.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
            selectionLayout.visibility = View.GONE
            calculatorLayout.visibility = View.VISIBLE
            currentInput = ""
            display.text = "0"
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        if (webView.visibility == View.VISIBLE || selectionLayout.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
            selectionLayout.visibility = View.GONE
            calculatorLayout.visibility = View.VISIBLE
            currentInput = ""
            display.text = "0"
        }
    }
}
