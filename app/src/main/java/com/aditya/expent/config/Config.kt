package com.aditya.expent.config

import android.content.Context
import com.aditya.expent.di.ConfigModule
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

interface ExpentApp

object Config {
    private var remoteConfigManager: RemoteConfigManager? = null

    fun init(context: Context) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, ConfigEntryPoint::class.java)
        remoteConfigManager = entryPoint.remoteConfigManager()
    }

    private val config: RemoteConfig
        get() = remoteConfigManager?.getConfig() 
            ?: throw IllegalStateException("Config not initialized. Call Config.init(context) first.")

    val app: AppConfig
        get() = config.app

    val server: ServerConfig
        get() = config.server

    val login: LoginConfig
        get() = config.login

    val onboarding: OnboardingConfig
        get() = config.onboarding

    val dashboard: DashboardConfig
        get() = config.dashboard

    val transactions: TransactionsConfig
        get() = config.transactions

    val categories: CategoriesConfig
        get() = config.categories

    val accounts: AccountsConfig
        get() = config.accounts

    val budgets: BudgetsConfig
        get() = config.budgets

    val analysis: AnalysisConfig
        get() = config.analysis

    val cashflow: CashflowConfig
        get() = config.cashflow

    val profile: ProfileConfig
        get() = config.profile

    val settings: SettingsConfig
        get() = config.settings

    val notifications: NotificationsConfig
        get() = config.notifications

    val common: CommonConfig
        get() = config.common

    val errors: ErrorsConfig
        get() = config.errors

    val theme: ThemeConfig
        get() = config.theme

    val defaultCategories: DefaultCategoriesConfig
        get() = config.defaultCategories

    val defaultAccounts: List<DefaultAccountConfig>
        get() = config.defaultAccounts

    val currencies: List<CurrencyConfig>
        get() = config.currencies

    val features: FeaturesConfig
        get() = config.features

    val validation: ValidationConfig
        get() = config.validation

    val localization: LocalizationConfig
        get() = config.localization

    val experiments: ExperimentsConfig
        get() = config.experiments

    fun isFeatureEnabled(screen: String, feature: String): Boolean {
        return when (screen) {
            "auth" -> when (feature) {
                "googleSignIn" -> features.auth.googleSignIn
                "testLogin" -> features.auth.testLogin
                "biometricAuth" -> features.auth.biometricAuth
                else -> false
            }
            "sync" -> when (feature) {
                "enabled" -> features.sync.enabled
                "autoSync" -> features.sync.autoSync
                "wifiOnly" -> features.sync.wifiOnly
                "backgroundSync" -> features.sync.backgroundSync
                else -> false
            }
            "ai" -> when (feature) {
                "enabled" -> features.ai.enabled
                "parseTransactions" -> features.ai.parseTransactions
                "categorizeTransactions" -> features.ai.categorizeTransactions
                "detectRecurring" -> features.ai.detectRecurring
                "provideInsights" -> features.ai.provideInsights
                else -> false
            }
            "widget" -> when (feature) {
                "enabled" -> features.widget.enabled
                "showBalance" -> features.widget.showBalance
                "showQuickAdd" -> features.widget.showQuickAdd
                else -> false
            }
            "debug" -> when (feature) {
                "enableLogs" -> features.debug.enableLogs
                "enableNetworkLogs" -> features.debug.enableNetworkLogs
                "showDebugMenu" -> features.debug.showDebugMenu
                "mockData" -> features.debug.mockData
                else -> false
            }
            "dashboard" -> when (feature) {
                "showBalance" -> dashboard.showBalance
                "showIncomeExpenseChart" -> dashboard.showIncomeExpenseChart
                "showRecentTransactions" -> dashboard.showRecentTransactions
                "showReminders" -> dashboard.showReminders
                "showQuickActions" -> dashboard.showQuickActions
                "enablePullToRefresh" -> dashboard.enablePullToRefresh
                else -> false
            }
            "transactions" -> when (feature) {
                "enableCategories" -> transactions.enableCategories
                "enableTags" -> transactions.enableTags
                "enableAttachments" -> transactions.enableAttachments
                "enableRecurring" -> transactions.enableRecurring
                "enableSplitTransactions" -> transactions.enableSplitTransactions
                else -> false
            }
            "categories" -> when (feature) {
                "enableCustomCategories" -> categories.enableCustomCategories
                "enableSubCategories" -> categories.enableSubCategories
                "defaultCategoriesEnabled" -> categories.defaultCategoriesEnabled
                else -> false
            }
            "accounts" -> when (feature) {
                "enableMultipleAccounts" -> accounts.enableMultipleAccounts
                "enableAccountTypes" -> accounts.enableAccountTypes
                "enableDefaultAccount" -> accounts.enableDefaultAccount
                else -> false
            }
            "budgets" -> when (feature) {
                "enableBudgetTracking" -> budgets.enableBudgetTracking
                "enableRollOver" -> budgets.enableRollOver
                "enableAlerts" -> budgets.enableAlerts
                else -> false
            }
            "analysis" -> when (feature) {
                "enableCharts" -> analysis.enableCharts
                "enableExport" -> analysis.enableExport
                else -> false
            }
            "cashflow" -> when (feature) {
                "enableForecasting" -> cashflow.enableForecasting
                "enableRecurringDetection" -> cashflow.enableRecurringDetection
                "showUpcomingBills" -> cashflow.showUpcomingBills
                else -> false
            }
            "profile" -> when (feature) {
                "enableThemeSelection" -> profile.enableThemeSelection
                "enableCurrencySelection" -> profile.enableCurrencySelection
                "enableLanguageSelection" -> profile.enableLanguageSelection
                "enableDataExport" -> profile.enableDataExport
                "enableDataDeletion" -> profile.enableDataDeletion
                "enableBackup" -> profile.enableBackup
                else -> false
            }
            "notifications" -> when (feature) {
                "enabled" -> notifications.enabled
                "reminders" -> notifications.types.reminders
                "budgetAlerts" -> notifications.types.budgetAlerts
                "billReminders" -> notifications.types.billReminders
                "weeklyReport" -> notifications.types.weeklyReport
                "monthlyReport" -> notifications.types.monthlyReport
                "tips" -> notifications.types.tips
                "promotions" -> notifications.types.promotions
                "push" -> notifications.channels.push
                "email" -> notifications.channels.email
                "inApp" -> notifications.channels.inApp
                else -> false
            }
            else -> false
        }
    }

    fun getText(screen: String, key: String): String {
        return when (screen) {
            "login" -> getLoginText(key)
            "onboarding" -> getOnboardingText(key)
            "dashboard" -> getDashboardText(key)
            "transactions" -> getTransactionsText(key)
            "categories" -> getCategoriesText(key)
            "accounts" -> getAccountsText(key)
            "budgets" -> getBudgetsText(key)
            "analysis" -> getAnalysisText(key)
            "cashflow" -> getCashflowText(key)
            "profile" -> getProfileText(key)
            "settings" -> getSettingsText(key)
            "notifications" -> getNotificationsText(key)
            "common" -> getCommonText(key)
            "errors" -> getErrorsText(key)
            else -> key
        }
    }

    private fun getLoginText(key: String): String = with(login) {
        when (key) {
            "title" -> title
            "subtitle" -> subtitle
            "loginButtonText" -> loginButtonText
            "testLoginButtonText" -> testLoginButtonText
            "privacyPolicyFirstLine" -> privacyPolicyFirstLine
            "privacyPolicyLinkText" -> privacyPolicyLinkText
            "andText" -> andText
            "termsOfServiceLinkText" -> termsOfServiceLinkText
            "biometricPromptTitle" -> biometricPromptTitle
            "biometricPromptSubtitle" -> biometricPromptSubtitle
            else -> key
        }
    }

    private fun getOnboardingText(key: String): String = with(onboarding) {
        when (key) {
            "welcome.title" -> welcome.title
            "welcome.subtitle" -> welcome.subtitle
            "welcome.getStarted" -> welcome.getStarted
            "currency.title" -> currency.title
            "currency.subtitle" -> currency.subtitle
            "currency.selectCurrency" -> currency.selectCurrency
            "categories.title" -> categories.title
            "categories.subtitle" -> categories.subtitle
            "categories.expenseCategories" -> categories.expenseCategories
            "categories.incomeCategories" -> categories.incomeCategories
            "categories.selectAll" -> categories.selectAll
            "categories.continueButton" -> categories.continueButton
            "accounts.title" -> accounts.title
            "accounts.subtitle" -> accounts.subtitle
            "accounts.addAccount" -> accounts.addAccount
            "accounts.skipForNow" -> accounts.skipForNow
            "budget.title" -> budget.title
            "budget.subtitle" -> budget.subtitle
            "budget.enterAmount" -> budget.enterAmount
            "budget.skipForNow" -> budget.skipForNow
            "complete.title" -> complete.title
            "complete.subtitle" -> complete.subtitle
            "complete.goToDashboard" -> complete.goToDashboard
            else -> key
        }
    }

    private fun getDashboardText(key: String): String = with(dashboard) {
        when (key) {
            "title" -> title
            "totalBalance" -> totalBalance
            "monthlyIncome" -> monthlyIncome
            "monthlyExpense" -> monthlyExpense
            "savings" -> savings
            "recentTransactions" -> recentTransactions
            "viewAll" -> viewAll
            "noTransactions" -> noTransactions
            "addFirstTransaction" -> addFirstTransaction
            "upcomingReminders" -> upcomingReminders
            "noReminders" -> noReminders
            "quickActions" -> quickActions
            "addExpense" -> addExpense
            "addIncome" -> addIncome
            "addTransfer" -> addTransfer
            "periodSelector" -> periodSelector
            "thisMonth" -> thisMonth
            "lastMonth" -> lastMonth
            "thisWeek" -> thisWeek
            "customRange" -> customRange
            else -> key
        }
    }

    private fun getTransactionsText(key: String): String = with(transactions) {
        when (key) {
            "title" -> title
            "addTransaction" -> addTransaction
            "editTransaction" -> editTransaction
            "deleteTransaction" -> deleteTransaction
            "deleteConfirmation" -> deleteConfirmation
            "type" -> type
            "expense" -> expense
            "income" -> income
            "transfer" -> transfer
            "amount" -> amount
            "category" -> category
            "account" -> account
            "date" -> date
            "note" -> note
            "tags" -> tags
            "attachments" -> attachments
            "recurring" -> recurring
            "recurringOptions" -> recurringOptions
            "noTransactions" -> noTransactions
            "filterBy" -> filterBy
            "sortBy" -> sortBy
            "dateRange" -> dateRange
            "amountRange" -> amountRange
            "categories" -> categories
            "accounts" -> accounts
            "transactionTypes" -> transactionTypes
            "clearFilters" -> clearFilters
            "applyFilters" -> applyFilters
            "total" -> total
            "count" -> count
            else -> key
        }
    }

    private fun getCategoriesText(key: String): String = with(categories) {
        when (key) {
            "title" -> title
            "addCategory" -> addCategory
            "editCategory" -> editCategory
            "deleteCategory" -> deleteCategory
            "deleteConfirmation" -> deleteConfirmation
            "name" -> name
            "icon" -> icon
            "color" -> color
            "type" -> type
            "parentCategory" -> parentCategory
            "noParent" -> noParent
            "subCategories" -> subCategories
            "noCategories" -> noCategories
            "defaultCategories" -> defaultCategories
            "customCategories" -> customCategories
            "expenseCategories" -> expenseCategories
            "incomeCategories" -> incomeCategories
            else -> key
        }
    }

    private fun getAccountsText(key: String): String = with(accounts) {
        when (key) {
            "title" -> title
            "addAccount" -> addAccount
            "editAccount" -> editAccount
            "deleteAccount" -> deleteAccount
            "deleteConfirmation" -> deleteConfirmation
            "name" -> name
            "type" -> type
            "balance" -> balance
            "currency" -> currency
            "isDefault" -> isDefault
            "noAccounts" -> noAccounts
            else -> key
        }
    }

    private fun getBudgetsText(key: String): String = with(budgets) {
        when (key) {
            "title" -> title
            "addBudget" -> addBudget
            "editBudget" -> editBudget
            "deleteBudget" -> deleteBudget
            "deleteConfirmation" -> deleteConfirmation
            "name" -> name
            "amount" -> amount
            "period" -> period
            "category" -> category
            "alertThreshold" -> alertThreshold
            "spent" -> spent
            "remaining" -> remaining
            "progress" -> progress
            "overBudget" -> overBudget
            "nearLimit" -> nearLimit
            "onTrack" -> onTrack
            "noBudgets" -> noBudgets
            "periods.weekly" -> periods.weekly
            "periods.monthly" -> periods.monthly
            "periods.yearly" -> periods.yearly
            else -> key
        }
    }

    private fun getAnalysisText(key: String): String = with(analysis) {
        when (key) {
            "title" -> title
            "incomeVsExpense" -> incomeVsExpense
            "categoryBreakdown" -> categoryBreakdown
            "trend" -> trend
            "cashFlow" -> cashFlow
            "export" -> export
            "dateRange" -> dateRange
            "chartType" -> chartType
            "noData" -> noData
            "insights" -> insights
            "topCategories" -> topCategories
            "averageSpending" -> averageSpending
            "savingsRate" -> savingsRate
            else -> key
        }
    }

    private fun getCashflowText(key: String): String = with(cashflow) {
        when (key) {
            "title" -> title
            "upcoming" -> upcoming
            "past" -> past
            "forecast" -> forecast
            "recurring" -> recurring
            "bills" -> bills
            "income" -> income
            "expenses" -> expenses
            "netFlow" -> netFlow
            "noUpcoming" -> noUpcoming
            "addRecurring" -> addRecurring
            else -> key
        }
    }

    private fun getProfileText(key: String): String = with(profile) {
        when (key) {
            "title" -> title
            "personalInfo" -> personalInfo
            "name" -> name
            "email" -> email
            "phone" -> phone
            "preferences" -> preferences
            "theme" -> theme
            "currency" -> currency
            "language" -> language
            "notifications" -> notifications
            "dataManagement" -> dataManagement
            "exportData" -> exportData
            "deleteData" -> deleteData
            "backup" -> backup
            "about" -> about
            "version" -> version
            "privacyPolicy" -> privacyPolicy
            "termsOfService" -> termsOfService
            "logout" -> logout
            "logoutConfirmation" -> logoutConfirmation
            else -> key
        }
    }

    private fun getSettingsText(key: String): String = with(settings) {
        when (key) {
            "title" -> title
            "general" -> general
            "appearance" -> appearance
            "notifications" -> notifications
            "data" -> data
            "advanced" -> advanced
            "theme" -> theme
            "currency" -> currency
            "language" -> language
            "dateFormat" -> dateFormat
            "numberFormat" -> numberFormat
            "firstDayOfWeek" -> firstDayOfWeek
            "enableBiometric" -> enableBiometric
            "autoBackup" -> autoBackup
            "syncFrequency" -> syncFrequency
            "themeOptions.light" -> themeOptions.light
            "themeOptions.dark" -> themeOptions.dark
            "themeOptions.system" -> themeOptions.system
            else -> key
        }
    }

    private fun getNotificationsText(key: String): String = with(notifications) {
        when (key) {
            "title" -> title
            "reminders" -> reminders
            "budgetAlerts" -> budgetAlerts
            "weeklyReport" -> weeklyReport
            "monthlyReport" -> monthlyReport
            "tips" -> tips
            "promotions" -> promotions
            "enableAll" -> enableAll
            "disableAll" -> disableAll
            else -> key
        }
    }

    private fun getCommonText(key: String): String = with(common) {
        when (key) {
            "appName" -> appName
            "ok" -> ok
            "cancel" -> cancel
            "save" -> save
            "delete" -> delete
            "edit" -> edit
            "add" -> add
            "search" -> search
            "filter" -> filter
            "sort" -> sort
            "loading" -> loading
            "error" -> error
            "retry" -> retry
            "noData" -> noData
            "noInternet" -> noInternet
            "somethingWentWrong" -> somethingWentWrong
            "confirm" -> confirm
            "yes" -> yes
            "no" -> no
            "skip" -> skip
            "next" -> next
            "back" -> back
            "done" -> done
            "close" -> close
            "settings" -> settings
            "help" -> help
            "about" -> about
            "version" -> version
            else -> key
        }
    }

    private fun getErrorsText(key: String): String = with(errors) {
        when (key) {
            "networkError" -> networkError
            "serverError" -> serverError
            "unauthorized" -> unauthorized
            "forbidden" -> forbidden
            "notFound" -> notFound
            "validationError" -> validationError
            "parseError" -> parseError
            "timeoutError" -> timeoutError
            "unknownError" -> unknownError
            "syncError" -> syncError
            "storageError" -> storageError
            else -> key
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConfigEntryPoint {
    fun remoteConfigManager(): RemoteConfigManager
}