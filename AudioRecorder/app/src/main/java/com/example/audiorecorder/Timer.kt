package com.example.audiorecorder

import android.os.Handler
import android.os.Looper

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
            listner.onTimerTick(duration.toString())
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


}