package com.xentoryx.expensey.core.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xentoryx.expensey.R
import com.xentoryx.expensey.app.MainActivity
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.presentation.util.NotificationScheduler
import com.xentoryx.expensey.core.storage.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.util.Locale

class DailyNotificationReceiver : BroadcastReceiver(), KoinComponent {

    private val tokenManager: TokenManager by inject()
    private val transactionDao: TransactionDao by inject()
    private val accountDao: AccountDao by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("DailyNotificationReceiver", "Received intent with action: $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = tokenManager.notificationEnabled.first()
                val hour = tokenManager.notificationHour.first()
                val minute = tokenManager.notificationMinute.first()

                if (enabled) {
                    if (action == Intent.ACTION_BOOT_COMPLETED) {
                        NotificationScheduler.scheduleDailyNotification(context, hour, minute)
                        Log.d("DailyNotificationReceiver", "Rescheduled daily notification after boot.")
                    } else {
                        showDailySummaryNotification(context)
                        NotificationScheduler.scheduleDailyNotification(context, hour, minute)
                        Log.d("DailyNotificationReceiver", "Triggered and rescheduled daily notification.")
                    }
                } else {
                    Log.d("DailyNotificationReceiver", "Notifications are disabled. Doing nothing.")
                }
            } catch (e: Exception) {
                Log.e("DailyNotificationReceiver", "Error in DailyNotificationReceiver flow", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showDailySummaryNotification(context: Context) {
        val todayStr = LocalDate.now().toString()
        val txs = transactionDao.getTransactionsByDate(todayStr)

        var totalIncome = 0.0
        var totalExpense = 0.0

        txs.forEach { tx ->
            when (tx.type.uppercase(Locale.US)) {
                "INCOME" -> totalIncome += tx.amount
                "EXPENSE" -> totalExpense += tx.amount
            }
        }

        val accounts = accountDao.getAccounts()
        val totalBalance = accounts.sumOf { it.balance }

        val recentTx = transactionDao.getRecentTransactions()
        
        // Calculate savings rate based on all transactions
        var totalIncomeAll = 0.0
        var totalExpenseAll = 0.0
        recentTx.forEach { tx ->
            when (tx.type.uppercase(Locale.US)) {
                "INCOME" -> totalIncomeAll += tx.amount
                "EXPENSE" -> totalExpenseAll += tx.amount
            }
        }
        val savingsRate = if (totalIncomeAll > 0.0) {
            val netSavings = totalIncomeAll - totalExpenseAll
            if (netSavings > 0.0) (netSavings / totalIncomeAll) * 100.0 else 0.0
        } else {
            0.0
        }

        var current = totalBalance
        val points = mutableListOf<Double>()
        points.add(current)
        recentTx.forEach { tx ->
            val isExpense = tx.type.uppercase(Locale.US) == "EXPENSE"
            if (isExpense) {
                current += tx.amount
            } else if (tx.type.uppercase(Locale.US) == "INCOME") {
                current -= tx.amount
            }
            points.add(current)
        }
        val displayPoints = if (points.size >= 2) points.reversed() else listOf(1000.0, 1200.0, 1100.0, 1500.0, 1350.0, 1800.0)
        
        val bitmapWidth = 800
        val bitmapHeight = 420
        val sparklineBitmap = drawNotificationCard(
            totalBalance = totalBalance,
            savingsRate = savingsRate,
            points = displayPoints,
            todayIncome = totalIncome,
            todayExpense = totalExpense,
            width = bitmapWidth,
            height = bitmapHeight
        )

        val title = "📊 Daily Finance Summary"
        val contentText = "Total Balance: $${String.format(Locale.US, "%,.2f", totalBalance)} • Today: +$${String.format(Locale.US, "%,.0f", totalIncome)} | -$${String.format(Locale.US, "%,.0f", totalExpense)}"

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "expensey_notifications"
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(sparklineBitmap)
                    .setSummaryText(contentText)
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Expensey Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(1002, notificationBuilder.build())
    }

    private fun drawNotificationCard(
        totalBalance: Double,
        savingsRate: Double,
        points: List<Double>,
        todayIncome: Double,
        todayExpense: Double,
        width: Int = 800,
        height: Int = 420
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw rounded container background
        val bgPaint = Paint().apply {
            color = 0xFF161522.toInt() // Dark slate color from the screenshot
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 32f, 32f, bgPaint)

        // 2. Draw "Net Worth" label
        val labelPaint = Paint().apply {
            color = 0xFFA0A0B0.toInt() // Grey color
            textSize = 20f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        canvas.drawText("Net Worth", 40f, 60f, labelPaint)

        // 3. Draw formatted Total Wealth (e.g. $230,099.00)
        val balancePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 46f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val balanceStr = "$${String.format(Locale.US, "%,.2f", totalBalance)}"
        canvas.drawText(balanceStr, 40f, 120f, balancePaint)

        // 4. Draw Savings Rate Badge on the top-right
        val badgeText = String.format(Locale.US, "%.1f%% Savings", savingsRate)
        val badgeTextPaint = Paint().apply {
            color = 0xFFFFB547.toInt() // Gold/yellow color
            textSize = 18f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val textWidth = badgeTextPaint.measureText(badgeText)
        val badgeWidth = textWidth + 30f
        val badgeHeight = 50f
        val badgeLeft = width - badgeWidth - 40f
        val badgeTop = 45f
        val badgeBottom = badgeTop + badgeHeight

        val badgePaint = Paint().apply {
            color = 0xFF241F18.toInt() // Dark gold tint background
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeBottom, 16f, 16f, badgePaint)
        
        // Draw percentage text inside the badge
        canvas.drawText(badgeText, badgeLeft + 15f, badgeTop + 32f, badgeTextPaint)

        // 5. Draw Sparkline Graph in the middle
        if (points.isNotEmpty()) {
            val minVal = points.minOrNull() ?: 0.0
            val maxVal = points.maxOrNull() ?: 1.0
            val range = if (maxVal == minVal) 1.0 else maxVal - minVal

            val path = Path()
            val fillPath = Path()

            val chartPaddingLeft = 40f
            val chartPaddingRight = 40f
            val chartWidth = width - chartPaddingLeft - chartPaddingRight
            val chartTop = 155f
            val chartBottom = 265f
            val chartHeight = chartBottom - chartTop

            points.forEachIndexed { index, value ->
                val x = chartPaddingLeft + (index.toFloat() / (points.size - 1)) * chartWidth
                val y = chartBottom - ((value - minVal) / range).toFloat() * chartHeight

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            // Close the fill path
            val lastX = chartPaddingLeft + chartWidth
            fillPath.lineTo(lastX, chartBottom)
            fillPath.lineTo(chartPaddingLeft, chartBottom)
            fillPath.close()

            // Draw gradient fill
            val fillPaint = Paint().apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f, chartTop, 0f, chartBottom,
                    0x4000C2A0.toInt(), // translucent green/cyan
                    0x00000000.toInt(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(fillPath, fillPaint)

            // Draw line stroke
            val strokePaint = Paint().apply {
                color = 0xFF00C2A0.toInt() // Green/cyan color from screenshot
                style = Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(path, strokePaint)
        }

        // 6. Draw Today's Income (Bottom-Left)
        val iconY = 335f
        val iconRadius = 24f
        val incomeX = 40f
        
        // Income circle background
        val incomeIconBgPaint = Paint().apply {
            color = 0x1A00C2A0.toInt() // 10% alpha green
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(incomeX + iconRadius, iconY, iconRadius, incomeIconBgPaint)

        // Income up arrow
        val incomeArrowPaint = Paint().apply {
            color = 0xFF00C2A0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        val cxIncome = incomeX + iconRadius
        canvas.drawLine(cxIncome, iconY + 10f, cxIncome, iconY - 10f, incomeArrowPaint)
        canvas.drawLine(cxIncome - 7f, iconY - 3f, cxIncome, iconY - 10f, incomeArrowPaint)
        canvas.drawLine(cxIncome + 7f, iconY - 3f, cxIncome, iconY - 10f, incomeArrowPaint)

        // Income label and value
        val textLabelPaint = Paint().apply {
            color = 0xFFA0A0B0.toInt()
            textSize = 18f
            isAntiAlias = true
        }
        canvas.drawText("Income", incomeX + iconRadius * 2 + 15f, iconY - 8f, textLabelPaint)

        val amountPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val incomeValStr = "$${String.format(Locale.US, "%,.2f", todayIncome)}"
        canvas.drawText(incomeValStr, incomeX + iconRadius * 2 + 15f, iconY + 22f, amountPaint)

        // 7. Draw Today's Expenses (Bottom-Right)
        val expenseX = 420f
        
        // Expense circle background
        val expenseIconBgPaint = Paint().apply {
            color = 0x1AFF6B6B.toInt() // 10% alpha red
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(expenseX + iconRadius, iconY, iconRadius, expenseIconBgPaint)

        // Expense down arrow
        val expenseArrowPaint = Paint().apply {
            color = 0xFFFF6B6B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        val cxExpense = expenseX + iconRadius
        canvas.drawLine(cxExpense, iconY - 10f, cxExpense, iconY + 10f, expenseArrowPaint)
        canvas.drawLine(cxExpense - 7f, iconY + 3f, cxExpense, iconY + 10f, expenseArrowPaint)
        canvas.drawLine(cxExpense + 7f, iconY + 3f, cxExpense, iconY + 10f, expenseArrowPaint)

        // Expense label and value
        canvas.drawText("Expenses", expenseX + iconRadius * 2 + 15f, iconY - 8f, textLabelPaint)

        val expenseValStr = "$${String.format(Locale.US, "%,.2f", todayExpense)}"
        canvas.drawText(expenseValStr, expenseX + iconRadius * 2 + 15f, iconY + 22f, amountPaint)

        return bitmap
    }
}
