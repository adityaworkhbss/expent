package com.aditya.expent.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aditya.expent.R
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.presentation.dashboard.DashboardActivity

class AddTransactionWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_add_transaction).apply {
                setOnClickPendingIntent(R.id.widget_add_expense, addTransactionIntent(context, TransactionType.EXPENSE))
                setOnClickPendingIntent(R.id.widget_add_income, addTransactionIntent(context, TransactionType.INCOME))
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun addTransactionIntent(context: Context, type: TransactionType): PendingIntent {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            action = ACTION_ADD_TRANSACTION
            putExtra(EXTRA_TRANSACTION_TYPE, type.name)
            // Start a fresh dashboard task so the intent always reaches onCreate; reusing an existing
            // task could just bring it to front without delivering the add request
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_ADD_TRANSACTION = "com.aditya.expent.action.ADD_TRANSACTION"
        const val EXTRA_TRANSACTION_TYPE = "extra_transaction_type"

        // Re-binds button intents; needed after a force-stop (e.g. reinstall) cancels the old PendingIntents
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AddTransactionWidget::class.java))
            if (ids.isNotEmpty()) {
                AddTransactionWidget().onUpdate(context, manager, ids)
            }
        }

        fun requestedType(intent: Intent?): TransactionType? {
            if (intent?.action != ACTION_ADD_TRANSACTION) return null
            return intent.getStringExtra(EXTRA_TRANSACTION_TYPE)
                ?.let { name -> TransactionType.values().firstOrNull { it.name == name } }
        }
    }
}
