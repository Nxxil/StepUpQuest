package com.example.stepupquest_tv

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.stepupquest_tv.ui.theme.StepUpQuestTheme

import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // It's generally recommended to initialize CastContext lazily
        // to avoid issues if Play Services is not yet ready.
        // However, the error suggests it's failing even at this point.
        try {
            CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            Log.e("MyApplication", "Failed to initialize CastContext", e)
            // Handle the exception, perhaps by disabling Cast features
        }
    }
}

// You also need an OptionsProvider
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        // Configure your CastOptions here
        // For a TV app, you might be a receiver, or you might be initiating casting
        // from the TV to another device (less common for a primary TV app screen).
        // If this TV app is intended to BE a Cast receiver, the setup is different.
        // If this TV app is trying to INITIATE casting, ensure a receiver app ID.
        return CastOptions.Builder()
            .setReceiverApplicationId("YOUR_RECEIVER_APP_ID") // Replace if you are a sender
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StepUpQuestTheme {
                // Puedes tener una UI simple aquí o lanzar TvStatsActivity directamente
                // Opción A: Lanzar TvStatsActivity directamente
                // startActivity(Intent(this, TvStatsActivity::class.java))
                // finish() // Cierra MainActivity para que no quede en la pila de atrás

                // Opción B: Mostrar un botón para ir a las estadísticas
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = {
                        startActivity(Intent(this@MainActivity, TvStatsActivity::class.java))
                    }) {
                        Text("Ver Estadísticas de Pasos")
                    }
                }
            }
        }
    }
}

