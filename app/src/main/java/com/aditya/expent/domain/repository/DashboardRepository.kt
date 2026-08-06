package com.aditya.expent.domain.repository

import com.aditya.expent.presentation.dashboard.DashboardState
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    suspend fun getDashboardSummary(): Flow<DashboardState>
}
