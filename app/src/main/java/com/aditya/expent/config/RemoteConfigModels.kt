package com.aditya.expent.config

import com.google.gson.annotations.SerializedName

data class RemoteConfig(
    @SerializedName("app") val app: AppConfig,
    @SerializedName("server") val server: ServerConfig,
    @SerializedName("login") val login: LoginConfig,
    @SerializedName("onboarding") val onboarding: OnboardingConfig,
    @SerializedName("dashboard") val dashboard: DashboardConfig,
    @SerializedName("transactions") val transactions: TransactionsConfig,
    @SerializedName("categories") val categories: CategoriesConfig,
    @SerializedName("accounts") val accounts: AccountsConfig,
    @SerializedName("budgets") val budgets: BudgetsConfig,
    @SerializedName("analysis") val analysis: AnalysisConfig,
    @SerializedName("cashflow") val cashflow: CashflowConfig,
    @SerializedName("profile") val profile: ProfileConfig,
    @SerializedName("settings") val settings: SettingsConfig,
    @SerializedName("notifications") val notifications: NotificationsConfig,
    @SerializedName("common") val common: CommonConfig,
    @SerializedName("errors") val errors: ErrorsConfig,
    @SerializedName("theme") val theme: ThemeConfig,
    @SerializedName("defaultCategories") val defaultCategories: DefaultCategoriesConfig,
    @SerializedName("defaultAccounts") val defaultAccounts: List<DefaultAccountConfig>,
    @SerializedName("currencies") val currencies: List<CurrencyConfig>,
    @SerializedName("features") val features: FeaturesConfig,
    @SerializedName("validation") val validation: ValidationConfig,
    @SerializedName("localization") val localization: LocalizationConfig,
    @SerializedName("experiments") val experiments: ExperimentsConfig
)

data class AppConfig(
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String,
    @SerializedName("buildNumber") val buildNumber: Int,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("minSupportedVersion") val minSupportedVersion: String,
    @SerializedName("forceUpdate") val forceUpdate: Boolean,
    @SerializedName("maintenanceMode") val maintenanceMode: Boolean,
    @SerializedName("maintenanceMessage") val maintenanceMessage: String
)

data class ServerConfig(
    @SerializedName("baseUrl") val baseUrl: String,
    @SerializedName("apiVersion") val apiVersion: String,
    @SerializedName("timeout") val timeout: Int,
    @SerializedName("retryCount") val retryCount: Int,
    @SerializedName("enableLogging") val enableLogging: Boolean,
    @SerializedName("endpoints") val endpoints: ServerEndpoints
) {
    fun getFullUrl(endpoint: String): String = "$baseUrl/$apiVersion$endpoint"
}

data class ServerEndpoints(
    @SerializedName("auth") val auth: String,
    @SerializedName("categories") val categories: String,
    @SerializedName("accounts") val accounts: String,
    @SerializedName("budgets") val budgets: String,
    @SerializedName("transactions") val transactions: String,
    @SerializedName("emis") val emis: String,
    @SerializedName("customization") val customization: String,
    @SerializedName("parseTransaction") val parseTransaction: String,
    @SerializedName("onboarding") val onboarding: String,
    @SerializedName("sync") val sync: String
)

data class LoginConfig(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("loginButtonText") val loginButtonText: String,
    @SerializedName("testLoginButtonText") val testLoginButtonText: String,
    @SerializedName("privacyPolicyFirstLine") val privacyPolicyFirstLine: String,
    @SerializedName("privacyPolicyLinkText") val privacyPolicyLinkText: String,
    @SerializedName("andText") val andText: String,
    @SerializedName("termsOfServiceLinkText") val termsOfServiceLinkText: String,
    @SerializedName("biometricPromptTitle") val biometricPromptTitle: String,
    @SerializedName("biometricPromptSubtitle") val biometricPromptSubtitle: String
)

data class OnboardingConfig(
    @SerializedName("welcome") val welcome: OnboardingStepConfig,
    @SerializedName("currency") val currency: OnboardingCurrencyConfig,
    @SerializedName("categories") val categories: OnboardingCategoriesConfig,
    @SerializedName("accounts") val accounts: OnboardingAccountsConfig,
    @SerializedName("budget") val budget: OnboardingBudgetConfig,
    @SerializedName("complete") val complete: OnboardingCompleteConfig,
    @SerializedName("steps") val steps: List<String>,
    @SerializedName("requiredSteps") val requiredSteps: List<String>,
    @SerializedName("enableSkip") val enableSkip: Boolean,
    @SerializedName("showTutorial") val showTutorial: Boolean
)

data class OnboardingStepConfig(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("getStarted") val getStarted: String
)

data class OnboardingCurrencyConfig(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("selectCurrency") val selectCurrency: String
)

data class OnboardingCategoriesConfig(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("expenseCategories") val expenseCategories: String,
    @SerializedName("incomeCategories") val incomeCategories: String,
    @SerializedName("selectAll") val selectAll: String,
    @SerializedName("continueButton") val continueButton: String
)

data class OnboardingAccountsConfig(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("addAccount") val addAccount: String,
    @SerializedName("skipForNow") val skipForNow: String
)

data class OnboardingBudgetConfig(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("enterAmount") val enterAmount: String,
    @SerializedName("skipForNow") val skipForNow: String
)

data class OnboardingCompleteConfig(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("goToDashboard") val goToDashboard: String
)

data class DashboardConfig(
    @SerializedName("title") val title: String,
    @SerializedName("totalBalance") val totalBalance: String,
    @SerializedName("monthlyIncome") val monthlyIncome: String,
    @SerializedName("monthlyExpense") val monthlyExpense: String,
    @SerializedName("savings") val savings: String,
    @SerializedName("recentTransactions") val recentTransactions: String,
    @SerializedName("viewAll") val viewAll: String,
    @SerializedName("noTransactions") val noTransactions: String,
    @SerializedName("addFirstTransaction") val addFirstTransaction: String,
    @SerializedName("upcomingReminders") val upcomingReminders: String,
    @SerializedName("noReminders") val noReminders: String,
    @SerializedName("quickActions") val quickActions: String,
    @SerializedName("addExpense") val addExpense: String,
    @SerializedName("addIncome") val addIncome: String,
    @SerializedName("addTransfer") val addTransfer: String,
    @SerializedName("periodSelector") val periodSelector: String,
    @SerializedName("thisMonth") val thisMonth: String,
    @SerializedName("lastMonth") val lastMonth: String,
    @SerializedName("thisWeek") val thisWeek: String,
    @SerializedName("customRange") val customRange: String,
    @SerializedName("showBalance") val showBalance: Boolean,
    @SerializedName("showIncomeExpenseChart") val showIncomeExpenseChart: Boolean,
    @SerializedName("showRecentTransactions") val showRecentTransactions: Boolean,
    @SerializedName("showReminders") val showReminders: Boolean,
    @SerializedName("showQuickActions") val showQuickActions: Boolean,
    @SerializedName("enablePullToRefresh") val enablePullToRefresh: Boolean,
    @SerializedName("defaultPeriod") val defaultPeriod: String
)

data class TransactionsConfig(
    @SerializedName("title") val title: String,
    @SerializedName("addTransaction") val addTransaction: String,
    @SerializedName("editTransaction") val editTransaction: String,
    @SerializedName("deleteTransaction") val deleteTransaction: String,
    @SerializedName("deleteConfirmation") val deleteConfirmation: String,
    @SerializedName("type") val type: String,
    @SerializedName("expense") val expense: String,
    @SerializedName("income") val income: String,
    @SerializedName("transfer") val transfer: String,
    @SerializedName("amount") val amount: String,
    @SerializedName("category") val category: String,
    @SerializedName("account") val account: String,
    @SerializedName("date") val date: String,
    @SerializedName("note") val note: String,
    @SerializedName("tags") val tags: String,
    @SerializedName("attachments") val attachments: String,
    @SerializedName("recurring") val recurring: String,
    @SerializedName("recurringOptions") val recurringOptions: String,
    @SerializedName("noTransactions") val noTransactions: String,
    @SerializedName("filterBy") val filterBy: String,
    @SerializedName("sortBy") val sortBy: String,
    @SerializedName("dateRange") val dateRange: String,
    @SerializedName("amountRange") val amountRange: String,
    @SerializedName("categories") val categories: String,
    @SerializedName("accounts") val accounts: String,
    @SerializedName("transactionTypes") val transactionTypes: String,
    @SerializedName("clearFilters") val clearFilters: String,
    @SerializedName("applyFilters") val applyFilters: String,
    @SerializedName("total") val total: String,
    @SerializedName("count") val count: String,
    @SerializedName("enableCategories") val enableCategories: Boolean,
    @SerializedName("enableTags") val enableTags: Boolean,
    @SerializedName("enableAttachments") val enableAttachments: Boolean,
    @SerializedName("enableRecurring") val enableRecurring: Boolean,
    @SerializedName("enableSplitTransactions") val enableSplitTransactions: Boolean,
    @SerializedName("maxAttachmentSize") val maxAttachmentSize: Long,
    @SerializedName("defaultSortBy") val defaultSortBy: String,
    @SerializedName("defaultSortOrder") val defaultSortOrder: String
)

data class CategoriesConfig(
    @SerializedName("title") val title: String,
    @SerializedName("addCategory") val addCategory: String,
    @SerializedName("editCategory") val editCategory: String,
    @SerializedName("deleteCategory") val deleteCategory: String,
    @SerializedName("deleteConfirmation") val deleteConfirmation: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String,
    @SerializedName("type") val type: String,
    @SerializedName("parentCategory") val parentCategory: String,
    @SerializedName("noParent") val noParent: String,
    @SerializedName("subCategories") val subCategories: String,
    @SerializedName("noCategories") val noCategories: String,
    @SerializedName("defaultCategories") val defaultCategories: String,
    @SerializedName("customCategories") val customCategories: String,
    @SerializedName("expenseCategories") val expenseCategories: String,
    @SerializedName("incomeCategories") val incomeCategories: String,
    @SerializedName("enableCustomCategories") val enableCustomCategories: Boolean,
    @SerializedName("enableSubCategories") val enableSubCategories: Boolean,
    @SerializedName("maxCategories") val maxCategories: Int,
    @SerializedName("defaultCategoriesEnabled") val defaultCategoriesEnabled: Boolean
)

data class AccountsConfig(
    @SerializedName("title") val title: String,
    @SerializedName("addAccount") val addAccount: String,
    @SerializedName("editAccount") val editAccount: String,
    @SerializedName("deleteAccount") val deleteAccount: String,
    @SerializedName("deleteConfirmation") val deleteConfirmation: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("balance") val balance: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("isDefault") val isDefault: String,
    @SerializedName("noAccounts") val noAccounts: String,
    @SerializedName("enableMultipleAccounts") val enableMultipleAccounts: Boolean,
    @SerializedName("enableAccountTypes") val enableAccountTypes: Boolean,
    @SerializedName("accountTypes") val accountTypes: List<String>,
    @SerializedName("enableDefaultAccount") val enableDefaultAccount: Boolean
)

data class BudgetsConfig(
    @SerializedName("title") val title: String,
    @SerializedName("addBudget") val addBudget: String,
    @SerializedName("editBudget") val editBudget: String,
    @SerializedName("deleteBudget") val deleteBudget: String,
    @SerializedName("deleteConfirmation") val deleteConfirmation: String,
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: String,
    @SerializedName("period") val period: String,
    @SerializedName("category") val category: String,
    @SerializedName("alertThreshold") val alertThreshold: String,
    @SerializedName("spent") val spent: String,
    @SerializedName("remaining") val remaining: String,
    @SerializedName("progress") val progress: String,
    @SerializedName("overBudget") val overBudget: String,
    @SerializedName("nearLimit") val nearLimit: String,
    @SerializedName("onTrack") val onTrack: String,
    @SerializedName("noBudgets") val noBudgets: String,
    @SerializedName("periods") val periods: BudgetPeriodsConfig,
    @SerializedName("enableBudgetTracking") val enableBudgetTracking: Boolean,
    @SerializedName("enableRollOver") val enableRollOver: Boolean,
    @SerializedName("enableAlerts") val enableAlerts: Boolean,
    @SerializedName("defaultAlertThreshold") val defaultAlertThreshold: Int,
    @SerializedName("defaultPeriod") val defaultPeriod: String
)

data class BudgetPeriodsConfig(
    @SerializedName("weekly") val weekly: String,
    @SerializedName("monthly") val monthly: String,
    @SerializedName("yearly") val yearly: String
)

data class AnalysisConfig(
    @SerializedName("title") val title: String,
    @SerializedName("incomeVsExpense") val incomeVsExpense: String,
    @SerializedName("categoryBreakdown") val categoryBreakdown: String,
    @SerializedName("trend") val trend: String,
    @SerializedName("cashFlow") val cashFlow: String,
    @SerializedName("export") val export: String,
    @SerializedName("dateRange") val dateRange: String,
    @SerializedName("chartType") val chartType: String,
    @SerializedName("noData") val noData: String,
    @SerializedName("insights") val insights: String,
    @SerializedName("topCategories") val topCategories: String,
    @SerializedName("averageSpending") val averageSpending: String,
    @SerializedName("savingsRate") val savingsRate: String,
    @SerializedName("enableCharts") val enableCharts: Boolean,
    @SerializedName("enableExport") val enableExport: Boolean,
    @SerializedName("exportFormats") val exportFormats: List<String>,
    @SerializedName("chartTypes") val chartTypes: List<String>,
    @SerializedName("defaultDateRange") val defaultDateRange: String,
    @SerializedName("defaultChartType") val defaultChartType: String
)

data class CashflowConfig(
    @SerializedName("title") val title: String,
    @SerializedName("upcoming") val upcoming: String,
    @SerializedName("past") val past: String,
    @SerializedName("forecast") val forecast: String,
    @SerializedName("recurring") val recurring: String,
    @SerializedName("bills") val bills: String,
    @SerializedName("income") val income: String,
    @SerializedName("expenses") val expenses: String,
    @SerializedName("netFlow") val netFlow: String,
    @SerializedName("noUpcoming") val noUpcoming: String,
    @SerializedName("addRecurring") val addRecurring: String,
    @SerializedName("enableForecasting") val enableForecasting: Boolean,
    @SerializedName("enableRecurringDetection") val enableRecurringDetection: Boolean,
    @SerializedName("forecastMonths") val forecastMonths: Int,
    @SerializedName("showUpcomingBills") val showUpcomingBills: Boolean
)

data class ProfileConfig(
    @SerializedName("title") val title: String,
    @SerializedName("personalInfo") val personalInfo: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("preferences") val preferences: String,
    @SerializedName("theme") val theme: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("language") val language: String,
    @SerializedName("notifications") val notifications: String,
    @SerializedName("dataManagement") val dataManagement: String,
    @SerializedName("exportData") val exportData: String,
    @SerializedName("deleteData") val deleteData: String,
    @SerializedName("backup") val backup: String,
    @SerializedName("about") val about: String,
    @SerializedName("version") val version: String,
    @SerializedName("privacyPolicy") val privacyPolicy: String,
    @SerializedName("termsOfService") val termsOfService: String,
    @SerializedName("logout") val logout: String,
    @SerializedName("logoutConfirmation") val logoutConfirmation: String,
    @SerializedName("enableThemeSelection") val enableThemeSelection: Boolean,
    @SerializedName("enableCurrencySelection") val enableCurrencySelection: Boolean,
    @SerializedName("enableLanguageSelection") val enableLanguageSelection: Boolean,
    @SerializedName("enableDataExport") val enableDataExport: Boolean,
    @SerializedName("enableDataDeletion") val enableDataDeletion: Boolean,
    @SerializedName("enableBackup") val enableBackup: Boolean,
    @SerializedName("availableThemes") val availableThemes: List<String>,
    @SerializedName("availableCurrencies") val availableCurrencies: List<String>,
    @SerializedName("availableLanguages") val availableLanguages: List<String>
)

data class SettingsConfig(
    @SerializedName("title") val title: String,
    @SerializedName("general") val general: String,
    @SerializedName("appearance") val appearance: String,
    @SerializedName("notifications") val notifications: String,
    @SerializedName("data") val data: String,
    @SerializedName("advanced") val advanced: String,
    @SerializedName("theme") val theme: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("language") val language: String,
    @SerializedName("dateFormat") val dateFormat: String,
    @SerializedName("numberFormat") val numberFormat: String,
    @SerializedName("firstDayOfWeek") val firstDayOfWeek: String,
    @SerializedName("enableBiometric") val enableBiometric: String,
    @SerializedName("autoBackup") val autoBackup: String,
    @SerializedName("syncFrequency") val syncFrequency: String,
    @SerializedName("themeOptions") val themeOptions: ThemeOptionsConfig
)

data class ThemeOptionsConfig(
    @SerializedName("light") val light: String,
    @SerializedName("dark") val dark: String,
    @SerializedName("system") val system: String
)

data class NotificationsConfig(
    @SerializedName("title") val title: String,
    @SerializedName("reminders") val reminders: String,
    @SerializedName("budgetAlerts") val budgetAlerts: String,
    @SerializedName("weeklyReport") val weeklyReport: String,
    @SerializedName("monthlyReport") val monthlyReport: String,
    @SerializedName("tips") val tips: String,
    @SerializedName("promotions") val promotions: String,
    @SerializedName("enableAll") val enableAll: String,
    @SerializedName("disableAll") val disableAll: String,
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("types") val types: NotificationTypesConfig,
    @SerializedName("channels") val channels: NotificationChannelsConfig
)

data class NotificationTypesConfig(
    @SerializedName("reminders") val reminders: Boolean,
    @SerializedName("budgetAlerts") val budgetAlerts: Boolean,
    @SerializedName("billReminders") val billReminders: Boolean,
    @SerializedName("weeklyReport") val weeklyReport: Boolean,
    @SerializedName("monthlyReport") val monthlyReport: Boolean,
    @SerializedName("tips") val tips: Boolean,
    @SerializedName("promotions") val promotions: Boolean
)

data class NotificationChannelsConfig(
    @SerializedName("push") val push: Boolean,
    @SerializedName("email") val email: Boolean,
    @SerializedName("inApp") val inApp: Boolean
)

data class CommonConfig(
    @SerializedName("appName") val appName: String,
    @SerializedName("ok") val ok: String,
    @SerializedName("cancel") val cancel: String,
    @SerializedName("save") val save: String,
    @SerializedName("delete") val delete: String,
    @SerializedName("edit") val edit: String,
    @SerializedName("add") val add: String,
    @SerializedName("search") val search: String,
    @SerializedName("filter") val filter: String,
    @SerializedName("sort") val sort: String,
    @SerializedName("loading") val loading: String,
    @SerializedName("error") val error: String,
    @SerializedName("retry") val retry: String,
    @SerializedName("noData") val noData: String,
    @SerializedName("noInternet") val noInternet: String,
    @SerializedName("somethingWentWrong") val somethingWentWrong: String,
    @SerializedName("confirm") val confirm: String,
    @SerializedName("yes") val yes: String,
    @SerializedName("no") val no: String,
    @SerializedName("skip") val skip: String,
    @SerializedName("next") val next: String,
    @SerializedName("back") val back: String,
    @SerializedName("done") val done: String,
    @SerializedName("close") val close: String,
    @SerializedName("settings") val settings: String,
    @SerializedName("help") val help: String,
    @SerializedName("about") val about: String,
    @SerializedName("version") val version: String
)

data class ErrorsConfig(
    @SerializedName("networkError") val networkError: String,
    @SerializedName("serverError") val serverError: String,
    @SerializedName("unauthorized") val unauthorized: String,
    @SerializedName("forbidden") val forbidden: String,
    @SerializedName("notFound") val notFound: String,
    @SerializedName("validationError") val validationError: String,
    @SerializedName("parseError") val parseError: String,
    @SerializedName("timeoutError") val timeoutError: String,
    @SerializedName("unknownError") val unknownError: String,
    @SerializedName("syncError") val syncError: String,
    @SerializedName("storageError") val storageError: String
)

data class ThemeConfig(
    @SerializedName("default") val defaultTheme: String,
    @SerializedName("colors") val colors: ThemeColors,
    @SerializedName("typography") val typography: ThemeTypography,
    @SerializedName("shapes") val shapes: ThemeShapes
)

data class ThemeColors(
    @SerializedName("light") val light: ColorPalette,
    @SerializedName("dark") val dark: ColorPalette
)

data class ColorPalette(
    @SerializedName("primary") val primary: String,
    @SerializedName("primaryVariant") val primaryVariant: String,
    @SerializedName("secondary") val secondary: String,
    @SerializedName("secondaryVariant") val secondaryVariant: String,
    @SerializedName("background") val background: String,
    @SerializedName("surface") val surface: String,
    @SerializedName("error") val error: String,
    @SerializedName("onPrimary") val onPrimary: String,
    @SerializedName("onSecondary") val onSecondary: String,
    @SerializedName("onBackground") val onBackground: String,
    @SerializedName("onSurface") val onSurface: String,
    @SerializedName("onError") val onError: String,
    @SerializedName("success") val success: String,
    @SerializedName("warning") val warning: String,
    @SerializedName("outline") val outline: String,
    @SerializedName("surfaceVariant") val surfaceVariant: String
)

data class ThemeTypography(
    @SerializedName("fontFamily") val fontFamily: String,
    @SerializedName("headlineLarge") val headlineLarge: TypographyStyle,
    @SerializedName("headlineMedium") val headlineMedium: TypographyStyle,
    @SerializedName("headlineSmall") val headlineSmall: TypographyStyle,
    @SerializedName("titleLarge") val titleLarge: TypographyStyle,
    @SerializedName("titleMedium") val titleMedium: TypographyStyle,
    @SerializedName("titleSmall") val titleSmall: TypographyStyle,
    @SerializedName("bodyLarge") val bodyLarge: TypographyStyle,
    @SerializedName("bodyMedium") val bodyMedium: TypographyStyle,
    @SerializedName("bodySmall") val bodySmall: TypographyStyle,
    @SerializedName("labelLarge") val labelLarge: TypographyStyle,
    @SerializedName("labelMedium") val labelMedium: TypographyStyle,
    @SerializedName("labelSmall") val labelSmall: TypographyStyle
)

data class TypographyStyle(
    @SerializedName("fontSize") val fontSize: Int,
    @SerializedName("fontWeight") val fontWeight: Int,
    @SerializedName("lineHeight") val lineHeight: Int
)

data class ThemeShapes(
    @SerializedName("small") val small: Int,
    @SerializedName("medium") val medium: Int,
    @SerializedName("large") val large: Int,
    @SerializedName("extraLarge") val extraLarge: Int
)

data class DefaultCategoriesConfig(
    @SerializedName("expense") val expense: List<DefaultCategoryConfig>,
    @SerializedName("income") val income: List<DefaultCategoryConfig>
)

data class DefaultCategoryConfig(
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String,
    @SerializedName("subCategories") val subCategories: List<String>
)

data class DefaultAccountConfig(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String,
    @SerializedName("isDefault") val isDefault: Boolean
)

data class CurrencyConfig(
    @SerializedName("code") val code: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("decimalPlaces") val decimalPlaces: Int,
    @SerializedName("symbolPosition") val symbolPosition: String
)

data class FeaturesConfig(
    @SerializedName("auth") val auth: AuthFeatures,
    @SerializedName("sync") val sync: SyncFeatures,
    @SerializedName("ai") val ai: AIFeatures,
    @SerializedName("widget") val widget: WidgetFeatures,
    @SerializedName("debug") val debug: DebugFeatures
)

data class AuthFeatures(
    @SerializedName("googleSignIn") val googleSignIn: Boolean,
    @SerializedName("testLogin") val testLogin: Boolean,
    @SerializedName("biometricAuth") val biometricAuth: Boolean,
    @SerializedName("sessionTimeout") val sessionTimeout: Long
)

data class SyncFeatures(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("autoSync") val autoSync: Boolean,
    @SerializedName("syncInterval") val syncInterval: Long,
    @SerializedName("wifiOnly") val wifiOnly: Boolean,
    @SerializedName("backgroundSync") val backgroundSync: Boolean,
    @SerializedName("conflictResolution") val conflictResolution: String
)

data class AIFeatures(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("parseTransactions") val parseTransactions: Boolean,
    @SerializedName("categorizeTransactions") val categorizeTransactions: Boolean,
    @SerializedName("detectRecurring") val detectRecurring: Boolean,
    @SerializedName("provideInsights") val provideInsights: Boolean,
    @SerializedName("modelVersion") val modelVersion: String
)

data class WidgetFeatures(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("showBalance") val showBalance: Boolean,
    @SerializedName("showQuickAdd") val showQuickAdd: Boolean,
    @SerializedName("refreshInterval") val refreshInterval: Long
)

data class DebugFeatures(
    @SerializedName("enableLogs") val enableLogs: Boolean,
    @SerializedName("enableNetworkLogs") val enableNetworkLogs: Boolean,
    @SerializedName("showDebugMenu") val showDebugMenu: Boolean,
    @SerializedName("mockData") val mockData: Boolean
)

data class ValidationConfig(
    @SerializedName("transaction") val transaction: TransactionValidation,
    @SerializedName("category") val category: CategoryValidation,
    @SerializedName("account") val account: AccountValidation,
    @SerializedName("budget") val budget: BudgetValidation
)

data class TransactionValidation(
    @SerializedName("minAmount") val minAmount: Double,
    @SerializedName("maxAmount") val maxAmount: Double,
    @SerializedName("maxNoteLength") val maxNoteLength: Int,
    @SerializedName("maxTagLength") val maxTagLength: Int,
    @SerializedName("maxTags") val maxTags: Int
)

data class CategoryValidation(
    @SerializedName("minNameLength") val minNameLength: Int,
    @SerializedName("maxNameLength") val maxNameLength: Int,
    @SerializedName("maxSubCategories") val maxSubCategories: Int
)

data class AccountValidation(
    @SerializedName("minNameLength") val minNameLength: Int,
    @SerializedName("maxNameLength") val maxNameLength: Int,
    @SerializedName("minBalance") val minBalance: Double,
    @SerializedName("maxBalance") val maxBalance: Double
)

data class BudgetValidation(
    @SerializedName("minAmount") val minAmount: Double,
    @SerializedName("maxAmount") val maxAmount: Double,
    @SerializedName("minAlertThreshold") val minAlertThreshold: Int,
    @SerializedName("maxAlertThreshold") val maxAlertThreshold: Int
)

data class LocalizationConfig(
    @SerializedName("defaultLanguage") val defaultLanguage: String,
    @SerializedName("supportedLanguages") val supportedLanguages: List<String>,
    @SerializedName("rtlLanguages") val rtlLanguages: List<String>,
    @SerializedName("dateFormats") val dateFormats: DateFormatsConfig,
    @SerializedName("timeFormats") val timeFormats: TimeFormatsConfig
)

data class DateFormatsConfig(
    @SerializedName("short") val shortFormat: String,
    @SerializedName("medium") val mediumFormat: String,
    @SerializedName("long") val longFormat: String,
    @SerializedName("full") val fullFormat: String
)

data class TimeFormatsConfig(
    @SerializedName("short") val shortFormat: String,
    @SerializedName("medium") val mediumFormat: String,
    @SerializedName("long") val longFormat: String
)

data class ExperimentsConfig(
    @SerializedName("newDashboardLayout") val newDashboardLayout: ExperimentConfig,
    @SerializedName("aiInsights") val aiInsights: ExperimentConfig,
    @SerializedName("advancedBudgeting") val advancedBudgeting: ExperimentConfig,
    @SerializedName("sharedBudgets") val sharedBudgets: ExperimentConfig,
    @SerializedName("investmentTracking") val investmentTracking: ExperimentConfig
)

data class ExperimentConfig(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("rolloutPercentage") val rolloutPercentage: Int
)