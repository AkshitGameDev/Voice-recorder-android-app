package com.example.audiorecorder

import android.os.Handler
import android.os.Looper
import kotlin.time.Duration.Companion.minutes

class Timer(listner: onTimerTickListner) {
    interface onTimerTickListner{
        fun onTimerTick(duration: String)
    }

    private var handler = Handler(Looper.getMainLooper())
    private  lateinit var runnable: Runnable

    private var duration = 0L
    private var delay = 100L

    init {
        runnable = kotlinx.coroutines.Runnable {
            duration +=delay
            handler.postDelayed(runnable, delay)
            listner.onTimerTick(format())
        }
    }

     fun start(){
        handler.postDelayed(runnable, delay)
    }

     fun pause(){
        handler.removeCallbacks(runnable)
    }
     fun stop(){
        handler.removeCallbacks(runnable)
        duration = 0L
    }

    fun format(): String{
        val millis: Long = duration % 1000
        val secons: Long = (duration / 1000) % 60
        val minutes: Long = (duration / (1000 * 60)) % 60
        val hours: Long = (duration / (1000 * 60 * 60)) % 60

        var formatted: String = if(hours > 0)
            "%02d:%02d:%02d.%02d".format(hours,minutes,secons,millis/10)
        else
            "%02d:%02d.%02d".format(minutes,secons,millis/10)

        return formatted
    }


}