package com.example.backrecord.activities

import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.backrecord.R
import com.nanchen.screenrecordhelper.ScreenRecordHelper
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {

    private var screenRecordHelper: ScreenRecordHelper? = null
    private val afdd: AssetFileDescriptor by lazy { assets.openFd("test.aac") }

    private lateinit var countDownTimer: CountDownTimer
    private lateinit var timerTextView: TextView
    private var elapsedTime: Long = 0

    private fun startTimer() {
        elapsedTime = 0
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            // 第一个参数是总时间，这里用 Long.MAX_VALUE 表示无限计时
            // 第二个参数是间隔时间
            override fun onTick(millisUntilFinished: Long) {
                // 每隔一秒执行一次
                elapsedTime += 1000
                updateTimerText()
            }

            override fun onFinish() {
                // This won't be called as we're using Long.MAX_VALUE
            }
        }

        countDownTimer.start()
    }

    private fun stopTimer() {
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
            updateTimerText()
        }
    }

    private fun updateTimerText() {
        val seconds = elapsedTime / 1000
        timerTextView.text = "计时: $seconds 秒"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnStart  = findViewById<Button>(R.id.btnStart)
        timerTextView = findViewById(R.id.text)

        btnStart.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (screenRecordHelper == null) {
                    screenRecordHelper = ScreenRecordHelper(this, object : ScreenRecordHelper.OnVideoRecordListener {
                        override fun onBeforeRecord() {
                        }

                        override fun onStartRecord() {
                            play()
                            startTimer()
                        }

                        override fun onCancelRecord() {
                            releasePlayer()
                        }

                        override fun onEndRecord() {
                            releasePlayer()
                        }

                    })
                }
                screenRecordHelper?.apply {
                    if (!isRecording) {
                        // 如果你想录制音频（一定会有环境音量），你可以打开下面这个限制,并且使用不带参数的 stopRecord()
//                        recordAudio = true
                        startRecord()
                    }
                }
            } else {
                Toast.makeText(this@MainActivity.applicationContext, "sorry,your phone does not support recording screen", Toast.LENGTH_LONG).show()
            }
        }
        val btnStop  = findViewById<Button>(R.id.btnStop);
        btnStop.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopTimer()
                screenRecordHelper?.apply {
                    if (isRecording) {
                        if (mediaPlayer != null) {
                            // 如果选择带参数的 stop 方法，则录制音频无效
                            stopRecord(mediaPlayer!!.duration.toLong(), 15 * 1000, afdd)
                        } else {
                            stopRecord()
                        }
                    }
                }
            }
        }
    }

    private fun play() {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.apply {
                this.reset()
                this.setDataSource(afdd.fileDescriptor, afdd.startOffset, afdd.length)
                this.isLooping = true
                this.prepare()
                this.start()
            }
        } catch (e: Exception) {
            Log.d("nanchen2251", "播放音乐失败")
        } finally {

        }
    }

    // 音频播放
    private var mediaPlayer: MediaPlayer? = null

    private fun releasePlayer() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && data != null) {
            screenRecordHelper?.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            screenRecordHelper?.clearAll()
        }
        afdd.close()
        stopTimer()
        super.onDestroy()
    }


}
