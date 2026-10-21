package com.aditya.expent.domain.repository

interface FeedbackRepository {
    suspend fun submitFeedback(rating: Int, message: String)
}
