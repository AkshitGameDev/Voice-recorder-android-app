package com.example.audiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.onTimerTickListner {

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private var recorder: MediaRecorder? = null
    private lateinit var btnRecord: ImageButton
    private lateinit var tvText: TextView
    private lateinit var viabrator: Vibrator

    private var isRecording = false
    private var isPaused = false

    private var dirPath = ""
    private var filename = ""
    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {

        timer = Timer(this)
        viabrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        btnRecord = findViewById(R.id.btnRecord)
        tvText = findViewById(R.id.tvTimer)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) ==
                PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }

        btnRecord.setOnClickListener {
            when {
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }
            viabrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            permissionGranted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startRecording() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        dirPath = "${externalCacheDir?.absolutePath}/"
        val sdf = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss", Locale.getDefault())
        val date = sdf.format(Date())
        filename = "audio_recorder_$date"

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // AAC in MP4 → use .m4a extension (common)
            setOutputFile("$dirPath$filename.m4a")
            try {
                prepare()
            } catch (e: IOException) {}

            start()
        }

        btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false

        timer.start()
    }

    private fun pauseRecorder() {
        // pause() requires API 24+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
                isPaused = true
                btnRecord.setImageResource(R.drawable.ic_record)
                timer.pause()
            } catch (_: IllegalStateException) { }
        }
    }

    private fun resumeRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
                isPaused = false
                btnRecord.setImageResource(R.drawable.ic_pause)
                timer.start()
            } catch (_: IllegalStateException) { }
        }
    }
    private fun stopRecording(){
        timer.stop()
    }

    private fun stopAndRelease() {
        try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            timer
        } catch (_: Exception) { }
        recorder = null
        isRecording = false
        isPaused = false
    }

    override fun onStop() {
        super.onStop()
        if (isRecording || recorder != null) stopAndRelease()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (recorder != null) stopAndRelease()
    }

    override fun onTimerTick(duration: String) {
        tvText.text = duration;

    }
}
