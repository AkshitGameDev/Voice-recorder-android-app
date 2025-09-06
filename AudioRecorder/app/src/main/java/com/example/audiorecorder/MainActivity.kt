package com.example.audiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.onTimerTickListner {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<android.widget.LinearLayout>
    private lateinit var vibrator: Vibrator
    private lateinit var timer: Timer

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var isPaused = false
    private var dirPath = ""
    private var filename = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init AFTER setContentView
        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        bottomSheetBehaviour = BottomSheetBehavior.from(binding.bottomSheet).apply {
            peekHeight = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) ==
                PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }

        // Listeners
        binding.btnRecord.setOnClickListener {
            when {
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        binding.btnList.setOnClickListener {
            Toast.makeText(this, "List button", Toast.LENGTH_SHORT).show()
        }

        binding.btnDone.setOnClickListener {
            Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show()
            stopRecording()
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE
            hideKeyboard(binding.fileNameInput)
        }

        binding.buttonCancle.setOnClickListener {
            File("$dirPath$filename.m4a").delete()
            dismiss()
        }

        binding.btnOk.setOnClickListener {
            dismiss()
            save()
        }

        binding.btnDelete.setOnClickListener {
            Toast.makeText(this, "Record Deleted", Toast.LENGTH_SHORT).show()
            stopRecording()
            File("$dirPath$filename.m4a").delete()
        }

        binding.btnDelete.isClickable = false
    }

    private fun save() {
        val newName = binding.fileNameInput.text?.toString()?.trim().orEmpty()
        if (newName.isNotEmpty() && newName != filename) {
            val from = File("$dirPath$filename.m4a")
            val to = File("$dirPath$newName.m4a")
            if (from.exists()) from.renameTo(to)
            filename = newName
        }
    }

    private fun dismiss() {
        binding.bottomSheetBG.visibility = View.GONE
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun hideKeyboard(v: View) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(v.windowToken, 0)
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        if (rc == REQUEST_CODE)
            permissionGranted = r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        dirPath = "${externalCacheDir?.absolutePath}/"
        filename = "audio_recorder_" + SimpleDateFormat(
            "yyyy.MM.dd_HH.mm.ss", Locale.getDefault()
        ).format(Date())

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.m4a")
            try { prepare() } catch (_: IOException) {}
            start()
        }

        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false
        timer.start()

        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)
        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE
    }

    private fun pauseRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
                isPaused = true
                binding.btnRecord.setImageResource(R.drawable.ic_record)
                timer.pause()
            } catch (_: IllegalStateException) {}
        }
    }

    private fun resumeRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
                isPaused = false
                binding.btnRecord.setImageResource(R.drawable.ic_pause)
                timer.start()
            } catch (_: IllegalStateException) {}
        }
    }

    private fun stopRecording() {
        timer.stop()
        recorder?.apply { try { stop() } catch (_: Exception) {}; release() }
        recorder = null
        isPaused = false
        isRecording = false

        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE

        binding.btnDelete.isClickable = false
        binding.btnDelete.setImageResource(R.drawable.ic_delete)
        binding.btnRecord.setImageResource(R.drawable.ic_record)
        binding.tvTimer.text = "00:00.00"
    }

    private fun stopAndRelease() {
        try { recorder?.apply { stop(); reset(); release() } } catch (_: Exception) {}
        recorder = null
        isRecording = false
        isPaused = false
    }

    override fun onStop() { super.onStop(); if (isRecording || recorder != null) stopAndRelease() }
    override fun onDestroy() { super.onDestroy(); if (recorder != null) stopAndRelease() }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
        val amp = recorder?.maxAmplitude ?: 0
        binding.waveFormView.addAptitude(amp.toFloat())
    }
}
