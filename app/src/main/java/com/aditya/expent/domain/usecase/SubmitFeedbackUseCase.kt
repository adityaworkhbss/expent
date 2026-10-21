package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.FeedbackRepository
import javax.inject.Inject

class SubmitFeedbackUseCase @Inject constructor(
    private val repository: FeedbackRepository
) {
    suspend operator fun invoke(rating: Int, message: String) {
        repository.submitFeedback(rating, message)
    }
}
