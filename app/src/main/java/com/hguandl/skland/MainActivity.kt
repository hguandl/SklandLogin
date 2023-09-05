package com.hguandl.skland

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import com.hguandl.skland.ui.theme.SklandLoginTheme

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/117.0"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SklandLoginTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LoginView("https://www.skland.com")
                }
            }
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun LoginView(url: String) {
    val clipboardManager = LocalClipboardManager.current

    Column {
        val sklandCredential = remember { mutableStateOf("") }

        AndroidView(factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.scrollBy(1024, 0)
                        view?.addJavascriptInterface(object {
                            @JavascriptInterface
                            fun submitSkCred(cred: String) {
                                sklandCredential.value = cred
                            }
                        }, "Android")

                        view?.evaluateJavascript(
                            """(function() {
                            const pollCred = setInterval(function () {
                                const cred = localStorage.getItem("SK_OAUTH_CRED_KEY");
                                if (cred) {
                                    Android.submitSkCred(cred);
                                    clearInterval(pollCred);
                                }
                            }, 500);
                        })();""".trimIndent(), null
                        )
                    }
                }

                settings.domStorageEnabled = true
                settings.javaScriptEnabled = true
                settings.userAgentString = USER_AGENT

                loadUrl(url)
            }
        }, update = {
            it.loadUrl(url)
        })

        if (sklandCredential.value.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {
                    sklandCredential.value = ""
                },
                title = { Text("Got Skland cred") },
                text = { Text("DO NOT SHARE to untrusted people") },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(sklandCredential.value))
                        },
                        content = {
                            Text("Copy")
                        }
                    )
                },
                dismissButton = {
                    Button(
                        onClick = {
                            sklandCredential.value = ""
                        },
                        content = {
                            Text("Cancel")
                        }
                    )
                }
            )
        }
    }
}
