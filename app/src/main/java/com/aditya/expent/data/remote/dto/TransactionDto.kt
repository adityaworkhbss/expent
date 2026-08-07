package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionQueryDto(
    @SerializedName("from")
    val from: String? = "2000-01-01",
    @SerializedName("to")
    val to: String? = "2099-12-31",
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("accountId")
    val accountId: String? = null,
    @SerializedName("page")
    val page: Int? = 1,
    @SerializedName("limit")
    val limit: Int? = 20
) {
    fun toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        from?.let { map["from"] = it }
        to?.let { map["to"] = it }
        type?.let { map["type"] = it }
        categoryId?.let { map["categoryId"] = it }
        accountId?.let { map["accountId"] = it }
        page?.let { map["page"] = it.toString() }
        limit?.let { map["limit"] = it.toString() }
        return map
    }
}

data class TransactionCategoryDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("color")
    val color: String? = null,
    @SerializedName("icon")
    val icon: String? = null
)

data class TransactionAccountDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String
)

data class TransactionResponseDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("userId")
    val userId: String? = null,
    @SerializedName("accountId")
    val accountId: String,
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("transferToAccountId")
    val transferToAccountId: String? = null,
    @SerializedName("type")
    val type: String,
    @SerializedName("amount")
    val amountRaw: Any? = null,
    @SerializedName("timestamp")
    val timestamp: String? = null,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("transactionDate")
    val transactionDateRaw: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("merchant")
    val merchant: String? = null,
    @SerializedName("paymentMethod")
    val paymentMethod: String? = null,
    @SerializedName("tags")
    val tags: List<String>? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("isSalary")
    val isSalary: Boolean? = null,
    @SerializedName("isDeleted")
    val isDeleted: Boolean? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("category")
    val category: TransactionCategoryDto? = null,
    @SerializedName("account")
    val account: TransactionAccountDto? = null,
    @SerializedName("transferToAccount")
    val transferToAccount: TransactionAccountDto? = null
) {
    val amount: String
        get() = amountRaw?.toString() ?: "0.0"

    val transactionDate: String
        get() = transactionDateRaw ?: date ?: timestamp ?: ""

    val resolvedNote: String?
        get() = note ?: notes ?: description ?: merchant
}

data class TransactionListDataDto(
    @SerializedName("items")
    val items: List<TransactionResponseDto> = emptyList(),
    @SerializedName("limit")
    val limit: Int = 10,
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("total")
    val total: Int = 0
)

data class MetaDto(
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("limit")
    val limit: Int = 20,
    @SerializedName("totalPages")
    val totalPages: Int = 1,
    @SerializedName("hasNextPage")
    val hasNextPage: Boolean = false,
    @SerializedName("hasPreviousPage")
    val hasPreviousPage: Boolean = false
)

data class PaginatedTransactionsResponseDto(
    @SerializedName("data")
    val dataList: List<TransactionResponseDto>? = null,
    @SerializedName("items")
    val itemsList: List<TransactionResponseDto>? = null,
    @SerializedName("meta")
    val meta: MetaDto? = null
) {
    val data: List<TransactionResponseDto>
        get() = dataList ?: itemsList ?: emptyList()

    constructor(data: List<TransactionResponseDto>, meta: MetaDto?) : this(
        dataList = data,
        itemsList = data,
        meta = meta
    )
}

data class CreateTransactionRequestDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("transactionDate")
    val transactionDate: String? = null,
    @SerializedName("accountId")
    val accountId: String,
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("transferToAccountId")
    val transferToAccountId: String? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("merchant")
    val merchant: String? = null,
    @SerializedName("paymentMethod")
    val paymentMethod: String? = null,
    @SerializedName("tags")
    val tags: List<String>? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("isSalary")
    val isSalary: Boolean? = null
)