package com.example.audiorecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveFormView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()
    private var amptitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()
    private var radius = 6f
    private var w = 9f

    private var sw = 0f;
    private var sh = 400f

    init {
        paint.color = Color.rgb(244,81,30)

        sw = resources.displayMetrics.widthPixels.toFloat()

    }
    fun addAptitude(amp: Float){
        amptitudes.add(amp)
        var left:Float = sw-w
        var top:Float = 0F
        var right: Float =( left + w)
        var bottom: Float =  amp

        spikes.add(RectF(left,top,right,bottom))


        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        spikes.forEach{

            println( "lenght of spikes "+ spikes.size)
            canvas.drawRoundRect(it, radius,radius,paint)
        }
    }

}