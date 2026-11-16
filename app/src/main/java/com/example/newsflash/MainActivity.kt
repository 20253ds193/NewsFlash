package com.example.newsflash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val pedirPermisoLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) {
            Toast.makeText(this, "Se requiere permiso para mostrar alertas", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verificarPermisoNotificaciones()
        iniciarTareaEnSegundoPlano()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaNoticias(context = this)
                }
            }
        }
    }

    private fun verificarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pedirPermisoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun iniciarTareaEnSegundoPlano() {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val tareaPeriodica = PeriodicWorkRequestBuilder<NewsWorker>(15, TimeUnit.MINUTES)
            .setConstraints(restricciones)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SincronizacionNoticias",
            ExistingPeriodicWorkPolicy.KEEP,
            tareaPeriodica
        )
    }
}

@Composable
fun PantallaNoticias(context: Context) {
    val prefs = remember { context.getSharedPreferences("NewsFlashPrefs", Context.MODE_PRIVATE) }
    var textoNoticia by remember {
        mutableStateOf(
            prefs.getString("ultimo_titulo", null)?.let { titulo ->
                val resumen = prefs.getString("ultimo_resumen", "")
                "$titulo\n\n$resumen"
            } ?: "Esperando sincronización..."
        )
    }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NEWS FLASH - ALERTAS",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD32F2F)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Última noticia sincronizada:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(text = textoNoticia, fontSize = 16.sp, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val trabajoUnico = OneTimeWorkRequestBuilder<NewsWorker>().build()
                WorkManager.getInstance(context).enqueue(trabajoUnico)
                Toast.makeText(context, "Consultando servidor...", Toast.LENGTH_SHORT).show()

                scope.launch {
                    delay(2000)
                    val titulo = prefs.getString("ultimo_titulo", null)
                    val resumen = prefs.getString("ultimo_resumen", "")
                    if (titulo != null) {
                        textoNoticia = "$titulo\n\n$resumen"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Forzar Sincronización (Prueba)")
        }
    }
}