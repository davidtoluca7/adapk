package com.ad.articulosdigitales

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private lateinit var splashImage: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var webView: WebView

    // Variables para reintentos
    private var retryCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val retryDelays = arrayOf(3000L, 5000L, 10000L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1) Referencias
        splashImage = findViewById(R.id.splash_image)
        progressBar = findViewById(R.id.progress_bar)
        webView = findViewById(R.id.webview)

        // 2) Cargar GIF con Glide de inmediato
        Glide.with(this)
            .asGif()
            .load(R.drawable.splash_animation) // Cambia si tu GIF se llama diferente
            .into(splashImage)

        // 3) Configurar WebView (caché activada)
        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        webView.visibility = View.GONE

        // 4) WebViewClient
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Ocultamos la imagen de splash
                splashImage.visibility = View.GONE
                // Mostramos el WebView
                webView.visibility = View.VISIBLE
                // Barra se oculta al terminar
                progressBar.visibility = View.GONE
            }

            // Si es un link a WhatsApp, Messenger, correo, se abre en la app externa
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                if (url.startsWith("whatsapp://")) {
                    openExternal(url, "No se encontró la aplicación de WhatsApp.")
                    return true
                }
                if (url.startsWith("mailto:")) {
                    openExternal(url, "No se encontró una aplicación de correo.")
                    return true
                }
                if (url.startsWith("https://m.me/")) {
                    openExternal(url, "No se encontró la aplicación de Messenger.")
                    return true
                }
                return false
            }

            // Cuando no hay internet
            override fun onReceivedError(view: WebView?, req: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, req, error)
                // Muestra el toast con tu texto extra
                Toast.makeText(
                    this@MainActivity,
                    "No hay conexión a internet. Reintentando conexión...",
                    Toast.LENGTH_SHORT
                ).show()

                handleNetworkError()
            }
        }

        // 5) WebChromeClient para la barra
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }

        // 6) Cargar la URL si no hay estado guardado
        if (savedInstanceState == null) {
            webView.loadUrl("https://articulosdigitales.com")
        }

        // 7) Botón atrás
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    // Abre apps externas
    private fun openExternal(url: String, errorMsg: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // Reintentos automáticos
    private fun handleNetworkError() {
        webView.visibility = View.GONE
        progressBar.visibility = View.GONE

        if (retryCount < retryDelays.size) {
            handler.postDelayed({
                if (isNetworkAvailable()) {
                    // Muestra un toast "Conexión establecida"
                    Toast.makeText(this, "Conexión establecida", Toast.LENGTH_SHORT).show()

                    retryCount = 0
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                    webView.reload()
                } else {
                    // Reintento -> toast "No hay conexión a internet. Reintentando conexión..."
                    Toast.makeText(
                        this,
                        "No hay conexión a internet. Reintentando conexión...",
                        Toast.LENGTH_SHORT
                    ).show()
                    handleNetworkError()
                }
            }, retryDelays[retryCount])
            retryCount++
        } else {
            Toast.makeText(
                this,
                "No se pudo conectar después de varios intentos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Checar conexión
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            return info != null && info.isConnected
        }
    }
}
