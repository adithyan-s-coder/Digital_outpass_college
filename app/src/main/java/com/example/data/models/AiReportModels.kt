package com.example.data.models

enum class ReportDatePreset(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom Range")
}

data class OutpassStatistics(
    val periodLabel: String,
    val preset: ReportDatePreset,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val department: String,
    val totalRequests: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val pendingCount: Int,
    val currentlyOutsideCount: Int,
    val returnedCount: Int,
    val lateReturnsCount: Int,
    val averageApprovalTimeMinutes: Long?,
    val averageReturnDelayMinutes: Long?,
    val topReasons: List<Pair<String, Int>>,
    val peakExitHours: String,
    val typeDistribution: Map<OutpassType, Int>,
    val recordsCount: Int
)

data class AiAnalysisResult(
    val executiveSummary: String,
    val keyObservations: List<String>,
    val importantTrends: List<String>,
    val administrativeInsights: List<String>,
    val isAiGenerated: Boolean,
    val fallbackWarning: String? = null
)

data class CompleteAiReport(
    val id: String,
    val generatedAt: Long,
    val generatedBy: User,
    val statistics: OutpassStatistics,
    val analysis: AiAnalysisResult
)
