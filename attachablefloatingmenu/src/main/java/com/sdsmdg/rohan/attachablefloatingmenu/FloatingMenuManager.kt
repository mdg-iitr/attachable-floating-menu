package com.sdsmdg.rohan.attachablefloatingmenu

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat.TRANSLUCENT
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import android.view.animation.OvershootInterpolator
import kotlin.math.abs


// context should be an activity context
class FloatingMenuManager(val context: Context) {

    val mWindowManager = (context as Activity).windowManager

    companion object {
        const val LOG_TAG = "FloatingMenuManager"
    }

    fun addView(v: View) {
        v.setOnTouchListener(object : View.OnTouchListener {

            val handler = Handler()
            var triggered = false
            var shouldRun = true
            var initialX = 0f
            var initialY = 0f
            var animSet = AnimatorSet()
            lateinit var longPressedRunnable: () -> Unit
            lateinit var menu: AttachableFloatingMenu

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                // process an event in the event stream
                return when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.d(LOG_TAG, "ACTION_DOWN")

                        initialX = event.rawX
                        initialY = event.rawY
                        shouldRun = true
                        longPressedRunnable = runnable@{
                            if (!shouldRun) return@runnable
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                            menu = AttachableFloatingMenu(context, event.rawX, event.rawY - 24f.toPixel())
                            menu.pivotX = event.rawX
                            menu.pivotY = event.rawY - 24f.toPixel()
                            menu.isEntering = true
                            animSet = AnimatorSet()
                            val animX = ObjectAnimator.ofFloat(menu, "scaleX", 0f, 1f)
                            val animY = ObjectAnimator.ofFloat(menu, "scaleY", 0f, 1f)
                            animX.duration = 150
                            animY.duration = 150
                            animX.interpolator = OvershootInterpolator()
                            animY.interpolator = OvershootInterpolator()
                            animX.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    menu.isEntering = false
                                }
                            })
                            val params = getDefaultParams()
                            params.dimAmount = 0.5f
                            mWindowManager.addView(menu, params)
                            animX.setAutoCancel(true)
                            animY.setAutoCancel(true)
                            animSet.play(animX).with(animY)
                            animSet.start()
                            triggered = true
                        }
                        handler.postDelayed(longPressedRunnable,
                                ViewConfiguration.getLongPressTimeout().toLong())
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        Log.d(LOG_TAG, "ACTION_MOVE")
                        if (abs(event.rawX - initialX) > 20 || abs(event.rawY - initialY) > 20
                        && !triggered) {
                            handler.removeCallbacks(longPressedRunnable)
                            shouldRun = false
                            view.parent.requestDisallowInterceptTouchEvent(false)
                            Log.d(LOG_TAG, "allowed 1")
                        }
                        if (triggered && menu.isDrawn && !menu.isEntering) {
                            // calculate and animate
                            menu.motionX = event.rawX
                            menu.motionY = event.rawY - 24f.toPixel()
                            return true
                        }
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.d(LOG_TAG, "ACTION_UP")
                        handler.removeCallbacks(longPressedRunnable)
                        shouldRun = false
                        view.parent.requestDisallowInterceptTouchEvent(false)
                        Log.d(LOG_TAG, "allowed 2")
                        if (triggered) {
                            // dismiss or perform action
                            triggered = false
                            animSet.cancel()
                            mWindowManager.removeView(menu)
                            return true
                        }
                        false
                    }
                    else -> {
                        Log.d(LOG_TAG, "${event.action}")
                        view.parent.requestDisallowInterceptTouchEvent(false)
                        Log.d(LOG_TAG, "allowed 3")
                        false
                    }

                }
            }
        })
    }

    private fun getDefaultParams() = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0,
            0,
            TYPE_APPLICATION, // this is the reason for crash if applicationContext is passed
            FLAG_HARDWARE_ACCELERATED or FLAG_DIM_BEHIND,
            TRANSLUCENT
    )
}
