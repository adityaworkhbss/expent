package com.aditya.expent.domain.repository

import com.aditya.expent.presentation.dashboard.DashboardState

interface DashboardRepository {
    suspend fun getDashboardSummary(): Result<DashboardState>
}
