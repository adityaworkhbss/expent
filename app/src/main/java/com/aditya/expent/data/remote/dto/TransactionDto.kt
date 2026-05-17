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
    val userId: String,
    @SerializedName("accountId")
    val accountId: String,
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("transferToAccountId")
    val transferToAccountId: String? = null,
    @SerializedName("type")
    val type: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("transactionDate")
    val transactionDate: String,
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
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("category")
    val category: TransactionCategoryDto? = null,
    @SerializedName("account")
    val account: TransactionAccountDto? = null,
    @SerializedName("transferToAccount")
    val transferToAccount: TransactionAccountDto? = null
)

data class MetaDto(
    @SerializedName("total")
    val total: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("totalPages")
    val totalPages: Int,
    @SerializedName("hasNextPage")
    val hasNextPage: Boolean,
    @SerializedName("hasPreviousPage")
    val hasPreviousPage: Boolean
)

data class PaginatedTransactionsResponseDto(
    @SerializedName("data")
    val data: List<TransactionResponseDto>,
    @SerializedName("meta")
    val meta: MetaDto
)

data class CreateTransactionRequestDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("transactionDate")
    val transactionDate: String,
    @SerializedName("accountId")
    val accountId: String,
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("transferToAccountId")
    val transferToAccountId: String? = null,
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