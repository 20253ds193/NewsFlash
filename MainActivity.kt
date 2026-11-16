package com.example.newsflash

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var tvUltimaNoticia: TextView

    private val pedirPermisoLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) {
            Toast.makeText(this, "Se requiere permiso para mostrar alertas", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvUltimaNoticia = findViewById(R.id.tvUltimaNoticia)
        val btnSincronizar = findViewById<Button>(R.id.btnSincronizarAhora)

        verificarPermisoNotificaciones()
        iniciarTareaEnSegundoPlano()
        actualizarInterfaz()

        btnSincronizar.setOnClickListener {
            val trabajoUnico = OneTimeWorkRequestBuilder<NewsWorker>().build()
            WorkManager.getInstance(this).enqueue(trabajoUnico)
            Toast.makeText(this, "Consultando servidor...", Toast.LENGTH_SHORT).show()
            tvUltimaNoticia.postDelayed({ actualizarInterfaz() }, 2000)
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarInterfaz()
    }

    private fun actualizarInterfaz() {
        val prefs = getSharedPreferences("NewsFlashPrefs", MODE_PRIVATE)
        val titulo = prefs.getString("ultimo_titulo", null)
        val resumen = prefs.getString("ultimo_resumen", null)

        if (titulo != null) {
            tvUltimaNoticia.text = "$titulo\n\n$resumen"
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

    // Programa la tarea periódica en segundo plano cada 15 minutos con restricciones de red y batería
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