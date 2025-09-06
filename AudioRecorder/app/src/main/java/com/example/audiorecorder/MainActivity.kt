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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.onTimerTickListner {

    private lateinit var timer: Timer
    private lateinit var vibrator: Vibrator

    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBG: View
    private lateinit var fileNameInput: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnOk: MaterialButton

    private lateinit var btnRecord: ImageButton
    private lateinit var btnList: ImageButton
    private lateinit var btnDone: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var tvText: TextView
    private lateinit var waveFormView: WaveFormView

    private var recorder: MediaRecorder? = null
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false
    private var isRecording = false
    private var isPaused = false
    private var dirPath = ""
    private var filename = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 2) INIT HERE (AFTER setContentView)
        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBG = findViewById(R.id.bottomSheetBG)
        fileNameInput = findViewById(R.id.fileNameInput)
        btnCancel = findViewById(R.id.buttonCancle)   // your id
        btnOk = findViewById(R.id.btnOk)

        btnRecord = findViewById(R.id.btnRecord)
        btnList = findViewById(R.id.btnList)
        btnDone = findViewById(R.id.btnDone)
        btnDelete = findViewById(R.id.btnDelete)
        tvText = findViewById(R.id.tvTimer)
        waveFormView = findViewById(R.id.waveFormView)

        bottomSheetBehaviour = BottomSheetBehavior.from(bottomSheet).apply {
            peekHeight = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

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
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        btnList.setOnClickListener {
            Toast.makeText(this, "List button", Toast.LENGTH_SHORT).show()
        }

        btnDone.setOnClickListener {
            Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show()
            stopRecording()
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBG.visibility = View.VISIBLE
            hideKeyboard(fileNameInput)
        }

        btnCancel.setOnClickListener {
            File("$dirPath$filename.m4a").delete() // match extension
            dismiss()
        }

        btnOk.setOnClickListener {
            dismiss()
            save()
        }

        btnDelete.setOnClickListener {
            Toast.makeText(this, "Record Deleted", Toast.LENGTH_SHORT).show()
            stopRecording()
            File("$dirPath$filename.m4a").delete()
        }

        btnDelete.isClickable = false
    }

    private fun save() {
        val newFileName = fileNameInput.text?.toString()?.trim().orEmpty()
        if (newFileName.isNotEmpty() && newFileName != filename) {
            val from = File("$dirPath$filename.m4a")
            val to = File("$dirPath$newFileName.m4a")
            if (from.exists()) from.renameTo(to)
            filename = newFileName
        }
    }

    private fun dismiss() {
        bottomSheetBG.visibility = View.GONE
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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
            setOutputFile("$dirPath$filename.m4a")
            try { prepare() } catch (_: IOException) {}
            start()
        }

        btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false

        timer.start()

        btnDelete.isClickable = true
        btnDelete.setImageResource(R.drawable.ic_delete)
        btnList.visibility = View.GONE
        btnDone.visibility = View.VISIBLE
    }

    private fun pauseRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
                isPaused = true
                btnRecord.setImageResource(R.drawable.ic_record)
                timer.pause()
            } catch (_: IllegalStateException) {}
        }
    }

    private fun resumeRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
                isPaused = false
                btnRecord.setImageResource(R.drawable.ic_pause)
                timer.start()
            } catch (_: IllegalStateException) {}
        }
    }

    private fun stopRecording() {
        timer.stop()
        recorder?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        recorder = null
        isPaused = false
        isRecording = false

        btnList.visibility = View.VISIBLE
        btnDone.visibility = View.GONE

        btnDelete.isClickable = false
        btnDelete.setImageResource(R.drawable.ic_delete)

        btnRecord.setImageResource(R.drawable.ic_record)
        tvText.text = "00:00.00"
    }

    private fun stopAndRelease() {
        try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (_: Exception) {}
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
        tvText.text = duration
        val amp = recorder?.maxAmplitude ?: 0
        waveFormView.addAptitude(amp.toFloat()) // cached view
    }
}
