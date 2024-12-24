package com.ad.articulosdigitales

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.view.View
import com.bumptech.glide.Glide
import android.widget.ProgressBar
import android.widget.TextView
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.os.Build
import android.webkit.WebChromeClient
import android.webkit.URLUtil
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import android.webkit.WebSettings

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBarBottom: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var retryButton: Button
    private var retryCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val retryDelays = arrayOf(3000L, 5000L, 10000L) // Tiempos de espera incrementales

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView = findViewById<ImageView>(R.id.splash_image)
        webView = findViewById(R.id.webview)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val progressText = findViewById<TextView>(R.id.progress_text)
        errorTextView = findViewById(R.id.error_text)
        progressBarBottom = findViewById(R.id.progress_bar_bottom)
        retryButton = findViewById(R.id.retry_button)

        // Cargar el GIF con Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.splash_animation)
            .into(imageView)

        // Configurar el WebView (oculto inicialmente)
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT // Respetar cabeceras de caché del servidor
        webView.visibility = View.GONE

        // Configurar el botón de reintento
        retryButton.setOnClickListener {
            retryButton.visibility = View.GONE
            errorTextView.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE
            webView.reload()
        }

        // WebViewClient para manejar la carga de la página y errores
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // La página ha terminado de cargar, ocultar el GIF y la barra de progreso, y mostrar el WebView
                imageView.visibility = View.GONE
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                if (url.startsWith("https://m.me/")) {
                    // Intent para abrir Facebook Messenger
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true // Indicar que la carga de la URL ha sido manejada

                } else if (url.startsWith("whatsapp://")) {
                    // Intent para abrir WhatsApp
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true // Indicar que la carga de la URL ha sido manejada

                } else if (url.startsWith("mailto:")) {
                    // Intent para abrir la aplicación de correo electrónico
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                    startActivity(intent)
                    return true // Indicar que la carga de la URL ha sido manejada
                }

                // Para otros esquemas, dejar que el WebView los cargue normalmente
                return false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (error?.errorCode == ERROR_HOST_LOOKUP ||
                        error?.errorCode == ERROR_CONNECT ||
                        error?.errorCode == ERROR_TIMEOUT ||
                        error?.errorCode == ERROR_UNKNOWN) {
                        handleNetworkError()
                        return
                    }
                }

                webView.visibility = View.GONE
                errorTextView.visibility = View.VISIBLE
                retryButton.visibility = View.VISIBLE
                errorTextView.text = "Error al cargar la página web."
            }

            // Mostrar la barra de progreso inferior al navegar por la web
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                progressBarBottom.visibility = View.VISIBLE
            }
        }

        // WebChromeClient para actualizar el progreso de la barra de progreso y el TextView
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                progressText.text = "$newProgress%"

                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                    progressText.visibility = View.GONE
                    progressBarBottom.visibility = View.GONE // Ocultar la barra inferior al terminar de cargar
                } else {
                    progressBar.visibility = View.VISIBLE
                    progressText.visibility = View.VISIBLE
                }
            }
        }

        // Mostrar la barra de progreso superior mientras se carga la página
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE

        // Cargar la URL en el WebView
        if (isValidUrl("https://articulosdigitales.com")) {
            webView.loadUrl("https://articulosdigitales.com")
        } else {
            // Mostrar un mensaje de error si la URL no es válida
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = "La URL no es válida."
        }

        // Configurar la acción de onBackPressed
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    progressBarBottom.visibility = View.VISIBLE // Mostrar la barra de progreso inferior al ir hacia atrás
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    private fun handleNetworkError() {
        webView.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        progressBarBottom.visibility = View.GONE

        if (retryCount < retryDelays.size) {

            errorTextView.text = "Reintentando en ${retryDelays[retryCount]/1000}..."
            handler.postDelayed({
                if (isNetworkAvailable()) {
                    retryCount = 0
                    errorTextView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE // Mostrar ProgressBar superior
                    progressText.visibility = View.VISIBLE // Mostrar TextView de porcentaje
                    webView.reload()
                } else {

                    handleNetworkError()
                }
            }, retryDelays[retryCount])
            retryCount++
        } else {
            // Máximo de reintentos alcanzado, mostrar mensaje de error
            errorTextView.text = "No se pudo conectar después de varios intentos."
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
    private fun isValidUrl(url: String): Boolean {
        return try {
            URLUtil.isValidUrl(url) && URLUtil.isNetworkUrl(url)
        } catch (e: Exception) {
            false
        }
    }
}