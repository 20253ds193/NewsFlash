package com.example.newsflash

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class NewsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Conexión con el servicio REST en la nube
            val urlNube = "https://65a1b2c3d4.mockapi.io/noticias/1"

            val client = OkHttpClient()
            val request = Request.Builder().url(urlNube).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return Result.retry()

            val jsonString = response.body?.string() ?: return Result.failure()
            val json = JSONObject(jsonString)

            val esUrgente = json.getBoolean("ultima_hora")
            val titulo = json.getString("titulo")
            val resumen = json.getString("resumen")

            // Evita duplicar notificaciones verificando el último título guardado
            val prefs = applicationContext.getSharedPreferences("NewsFlashPrefs", Context.MODE_PRIVATE)
            val ultimaNotificada = prefs.getString("ultimo_titulo", "")

            if (esUrgente && titulo != ultimaNotificada) {
                val helper = NotificationHelper(applicationContext)
                helper.mostrarNotificacion(titulo, resumen)

                prefs.edit()
                    .putString("ultimo_titulo", titulo)
                    .putString("ultimo_resumen", resumen)
                    .apply()
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}