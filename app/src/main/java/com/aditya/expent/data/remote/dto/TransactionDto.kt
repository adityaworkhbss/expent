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
    @SerializedName("type")
    val type: String,
    @SerializedName("amount")
    val amountRaw: Any? = null,
    // RFC 3339 timestamp of when the transaction happened
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    // Not sent by the API; filled from Room for display
    @SerializedName("category")
    val category: TransactionCategoryDto? = null,
    @SerializedName("account")
    val account: TransactionAccountDto? = null
) {
    val amount: String
        get() = amountRaw?.toString() ?: "0.0"
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
        get() = itemsList?.takeIf { it.isNotEmpty() } ?: dataList?.takeIf { it.isNotEmpty() } ?: itemsList ?: dataList ?: emptyList()

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
    // RFC 3339 timestamp; the backend also accepts yyyy-MM-dd
    @SerializedName("date")
    val date: String,
    @SerializedName("accountId")
    val accountId: String,
    @SerializedName("categoryId")
    val categoryId: String? = null
)