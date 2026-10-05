package com.lyx.phone.note.overlay

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lyx.phone.note.MainActivity
import com.lyx.phone.note.R
import com.lyx.phone.note.data.db.NumberNoteEntity
import com.lyx.phone.note.domain.CategoryDisplay
import com.lyx.phone.note.domain.NoteImportance

object IncomingOverlayController {
    private const val CHANNEL_ID = "incoming_note"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentView: View? = null
    private var phoneStateReceiver: BroadcastReceiver? = null
    private var statePollingTask: Runnable? = null

    fun show(context: Context, note: NumberNoteEntity) {
        val appContext = context.applicationContext
        val isLocked = appContext.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

        if (!Settings.canDrawOverlays(appContext)) {
            showFallbackNotification(appContext, note)
            return
        }

        val windowManager = appContext.getSystemService(WindowManager::class.java)
        val view = createView(appContext, note)
        val width = minOf(
            appContext.resources.displayMetrics.widthPixels - dp(appContext, 48),
            dp(appContext, 420)
        )
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
            horizontalMargin = 0.04f
        }

        if (isLocked) {
            showFallbackNotification(appContext, note)
        }

        runCatching {
            dismiss(appContext)
            registerPhoneStateReceiver(appContext)
            startPhoneStatePolling(appContext)
            windowManager.addView(view, params)
            currentView = view
            mainHandler.postDelayed({ dismiss(appContext) }, 60_000)
        }.onFailure {
            stopPhoneStateWatchers(appContext)
            showFallbackNotification(appContext, note)
        }
    }

    fun dismiss(context: Context) {
        val appContext = context.applicationContext
        val view = currentView
        if (view != null) {
            runCatching {
                appContext.getSystemService(WindowManager::class.java).removeView(view)
            }
        }
        currentView = null
        stopPhoneStateWatchers(appContext)
    }

    private fun createView(context: Context, note: NumberNoteEntity): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 22), dp(context, 18), dp(context, 22), dp(context, 16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 18).toFloat()
                setColor(Color.argb(242, 28, 31, 36))
            }
            elevation = dp(context, 12).toFloat()
        }
        val title = TextView(context).apply {
            text = note.displayLabel
            setTextColor(Color.WHITE)
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val body = TextView(context).apply {
            text = note.note.ifBlank { "本地备注：${note.displayLabel}" }
            setTextColor(Color.rgb(228, 232, 237))
            textSize = 19f
            maxLines = 3
            setPadding(0, dp(context, 6), 0, 0)
        }
        val meta = TextView(context).apply {
            val category = CategoryDisplay.labelOf(note.category)
            val importance = NoteImportance.fromName(note.importance).displayName
            text = "$category · $importance"
            setTextColor(Color.rgb(166, 214, 255))
            textSize = 16f
            setPadding(0, dp(context, 8), 0, 0)
        }
        val actions = TextView(context).apply {
            text = "关闭    编辑备注"
            setTextColor(Color.WHITE)
            textSize = 17f
            gravity = Gravity.END
            setPadding(0, dp(context, 12), 0, 0)
            setOnClickListener {
                dismiss(context)
                context.startActivity(editIntent(context, note).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        card.setOnClickListener { dismiss(context) }
        card.addView(title)
        card.addView(body)
        card.addView(meta)
        card.addView(actions)
        return card
    }

    private fun registerPhoneStateReceiver(context: Context) {
        unregisterPhoneStateReceiver(context)
        phoneStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_OFFHOOK || state == TelephonyManager.EXTRA_STATE_IDLE) {
                    dismiss(receiverContext)
                }
            }
        }
        runCatching {
            context.registerReceiver(phoneStateReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
        }.onFailure {
            phoneStateReceiver = null
        }
    }

    private fun startPhoneStatePolling(context: Context) {
        stopPhoneStatePolling()
        val appContext = context.applicationContext
        statePollingTask = object : Runnable {
            override fun run() {
                val callState = runCatching {
                    appContext.getSystemService(TelephonyManager::class.java).callState
                }.getOrDefault(TelephonyManager.CALL_STATE_RINGING)
                if (callState == TelephonyManager.CALL_STATE_IDLE || callState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    dismiss(appContext)
                    return
                }
                mainHandler.postDelayed(this, 1_000)
            }
        }
        mainHandler.postDelayed(statePollingTask!!, 1_000)
    }

    private fun stopPhoneStateWatchers(context: Context) {
        unregisterPhoneStateReceiver(context)
        stopPhoneStatePolling()
    }

    private fun unregisterPhoneStateReceiver(context: Context) {
        val receiver = phoneStateReceiver ?: return
        runCatching { context.unregisterReceiver(receiver) }
        phoneStateReceiver = null
    }

    private fun stopPhoneStatePolling() {
        statePollingTask?.let { mainHandler.removeCallbacks(it) }
        statePollingTask = null
    }

    private fun showFallbackNotification(context: Context, note: NumberNoteEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "来电备注提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            note.id.toInt(),
            editIntent(context, note),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(note.displayLabel)
            .setContentText(note.note.ifBlank { "本地来电备注" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(note.id.toInt(), notification) }
    }

    private fun editIntent(context: Context, note: NumberNoteEntity): Intent =
        Intent(context, MainActivity::class.java)
            .setAction("com.lyx.phone.note.EDIT_NOTE")
            .putExtra("normalizedNumber", note.normalizedNumber)

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
