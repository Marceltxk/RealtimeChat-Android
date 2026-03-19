package com.isacsilva.ufumessenger.util

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var startTime: Long = 0

    fun startRecording() {
        try {
            // Cria um arquivo temporário no cache do celular
            audioFile = File(context.cacheDir, "audio_msg_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
                startTime = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Erro ao iniciar gravação", e)
        }
    }

    // Retorna um par: O link (URI) do arquivo salvo e o tempo de duração
    fun stopRecording(): Pair<Uri?, Long> {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val duration = System.currentTimeMillis() - startTime
            val uri = audioFile?.let { Uri.fromFile(it) }

            Pair(uri, duration)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Erro ao parar gravação", e)
            mediaRecorder?.release()
            mediaRecorder = null
            Pair(null, 0L)
        }
    }
}