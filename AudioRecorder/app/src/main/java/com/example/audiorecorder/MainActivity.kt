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

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private var recorder: MediaRecorder? = null
    private lateinit var btnRecord: ImageButton
    private lateinit var btnList: ImageButton
    private lateinit var btnDone: ImageButton
    private lateinit var btnDelete: ImageButton


    private lateinit var tvText: TextView
    private lateinit var viabrator: Vibrator

    private var isRecording = false
    private var isPaused = false

    private var dirPath = ""
    private var filename = ""
    private lateinit var timer: Timer
    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<LinearLayout>
    private var bottomSheet = findViewById<LinearLayout>(R.id.bottomSheet)
    private var bottomSheetBG = findViewById<View>(R.id.bottomSheetBG)

    private var fileNameInput = findViewById<TextInputEditText>(R.id.fileNameInput)

    private var btnCancle = findViewById<MaterialButton>(R.id.buttonCancle)
    private var btnOk = findViewById<MaterialButton>(R.id.btnOk)


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

        bottomSheetBehaviour = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehaviour.peekHeight = 0
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED

        btnRecord = findViewById(R.id.btnRecord)
        btnList = findViewById(R.id.btnList)
        btnDone = findViewById(R.id.btnDone)
        btnDelete = findViewById(R.id.btnDelete)

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

        btnList.setOnClickListener{
            Toast.makeText(this,"List button", Toast.LENGTH_SHORT).show()
            //TODO
        }
        btnDone.setOnClickListener {
            Toast.makeText(this,"Record Saved", Toast.LENGTH_SHORT).show()
            stopRecording()

            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBG.visibility = View.VISIBLE
            hideKeyboard(fileNameInput)


        }

        btnCancle.setOnClickListener {
            File("$dirPath$filename.mp3").delete()
            dismiss()
        }

        btnOk.setOnClickListener {


            dismiss()
            save()
        }

        btnDelete.setOnClickListener{
            Toast.makeText(this,"Record Deleated", Toast.LENGTH_SHORT).show()
            stopRecording()
            File("$dirPath$filename.mp3")
        }


        btnDelete.isClickable = false
    }

    private fun save(){
        val newFileName = fileNameInput.text.toString()
        if(newFileName != filename){
            //todo
        }
    }

    private fun dismiss(){
        bottomSheetBG.visibility = View.GONE
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED

    }

    private fun hideKeyboard(view: View){
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


        btnDelete.isClickable = true
        btnDelete.setImageResource(R.drawable.ic_delete)
        btnList.visibility =View.GONE
        btnDone.visibility = View.VISIBLE
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

        recorder?.apply {
            stop()
            release()
        }
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
        val amp = recorder?.maxAmplitude ?: 0
       findViewById<WaveFormView>(R.id.waveFormView).addAptitude(amp.toFloat())

    }


}
