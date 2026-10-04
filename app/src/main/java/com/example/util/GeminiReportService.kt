package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.example.data.models.AiAnalysisResult
import com.example.data.models.OutpassStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiReportService {

    private const val TAG = "GeminiReportService"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun logW(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] WARN: $msg")
        }
    }

    private fun logE(tag: String, msg: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, msg, throwable)
        } catch (_: Throwable) {
            println("[$tag] ERROR: $msg ${throwable?.message.orEmpty()}")
        }
    }

    suspend fun analyzeStatistics(statistics: OutpassStatistics): AiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        // If no real API key is configured in AI Studio secrets, use intelligent fallback
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            logW(TAG, "GEMINI_API_KEY is not set or using placeholder. Generating local rule-based summary.")
            return@withContext generateLocalSummary(
                statistics,
                fallbackWarning = "AI summary is temporarily unavailable (API key not configured). The calculated report statistics are still available."
            )
        }

        try {
            val prompt = buildPrompt(statistics)
            val requestBodyJson = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                }
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply { put("parts", partsArray) })
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.8)
                    put("topK", 40)
                }
                put("generationConfig", generationConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)
            val requestUrl = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful || responseBody.isBlank()) {
                logE(TAG, "Gemini API request failed with HTTP ${response.code}: $responseBody")
                return@withContext generateLocalSummary(
                    statistics,
                    fallbackWarning = "AI summary is temporarily unavailable. The calculated report statistics are still available."
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext generateLocalSummary(
                    statistics,
                    fallbackWarning = "AI summary is temporarily unavailable. The calculated report statistics are still available."
                )
            }

            val text = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "")
                .orEmpty()

            if (text.isBlank()) {
                return@withContext generateLocalSummary(
                    statistics,
                    fallbackWarning = "AI summary is temporarily unavailable. The calculated report statistics are still available."
                )
            }

            parseGeminiResponse(text, statistics)
        } catch (e: Exception) {
            logE(TAG, "Error executing Gemini analysis: ${e.message}", e)
            generateLocalSummary(
                statistics,
                fallbackWarning = "AI summary is temporarily unavailable. The calculated report statistics are still available."
            )
        }
    }

    private fun buildPrompt(stats: OutpassStatistics): String {
        val reasonsList = if (stats.topReasons.isNotEmpty()) {
            stats.topReasons.joinToString("; ") { "${it.first} (${it.second} requests)" }
        } else {
            "No specific reasons reported"
        }

        val typeDistribution = if (stats.typeDistribution.isNotEmpty()) {
            stats.typeDistribution.entries.joinToString("; ") { "${it.key.displayName}: ${it.value}" }
        } else {
            "None"
        }

        val avgApprovalText = if (stats.averageApprovalTimeMinutes != null) {
            "${stats.averageApprovalTimeMinutes} minutes"
        } else {
            "N/A (No approvals in period)"
        }

        val avgDelayText = if (stats.averageReturnDelayMinutes != null) {
            "${stats.averageReturnDelayMinutes} minutes"
        } else {
            "0 minutes"
        }

        return """
You are an institutional compliance and campus digital outpass report assistant for collegiate Staff Advisors and Heads of Departments (HOD).
Generate an objective, highly readable administrative analysis based strictly on the provided real database statistics.

STRICT CONSTRAINTS:
1. ONLY utilize the numerical facts and information provided below. Do NOT invent or hallucinate unsupported statistics.
2. You CANNOT approve, reject, or modify outpasses, database records, or student records.
3. Be professional, concise, and focused on student safety, hostel gate compliance, and departmental accountability.

=== REAL OUTPASS AUDIT STATISTICS ===
- Report Period: ${stats.periodLabel}
- Department: ${stats.department}
- Total Outpass Requests: ${stats.totalRequests}
- Approved Requests: ${stats.approvedCount}
- Rejected Requests: ${stats.rejectedCount}
- Pending Requests: ${stats.pendingCount}
- Students Currently Outside Campus: ${stats.currentlyOutsideCount}
- Returned Students: ${stats.returnedCount}
- Late Returns Recorded: ${stats.lateReturnsCount}
- Average Staff/HOD Approval Duration: $avgApprovalText
- Average Return Delay: $avgDelayText
- Peak Campus Exit Period: ${stats.peakExitHours}
- Most Common Outpass Reasons: $reasonsList
- Requests by Outpass Category: $typeDistribution
=====================================

FORMAT YOUR OUTPUT EXACTLY USING THESE FOUR SECTION HEADERS:

EXECUTIVE SUMMARY
(Provide a crisp 2-3 sentence overview explaining overall student movement volume and status distribution for this period.)

KEY OBSERVATIONS
• (Observation on exit peak hours and student departure trends)
• (Observation on approvals vs pending/rejections)
• (Observation on return compliance and late returns)
• (Observation on primary student travel reasons)

IMPORTANT TRENDS
• (Specific pattern in request categories like Local vs Home leaves)
• (Insight into approval turnaround time)
• (Evaluation of gate check-in adherence)

ADMINISTRATIVE INSIGHTS
• (Actionable recommendation for Staff Advisors regarding pending reviews or late returns)
• (Institutional guidance for HOD regarding department outpass flow)
""".trimIndent()
    }

    private fun parseGeminiResponse(rawText: String, stats: OutpassStatistics): AiAnalysisResult {
        var execSummary = ""
        val observations = mutableListOf<String>()
        val trends = mutableListOf<String>()
        val insights = mutableListOf<String>()

        var currentSection = 0 // 1: Executive Summary, 2: Key Observations, 3: Important Trends, 4: Administrative Insights

        val lines = rawText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val upper = trimmed.uppercase()
            when {
                upper.contains("EXECUTIVE SUMMARY") -> {
                    currentSection = 1
                    continue
                }
                upper.contains("KEY OBSERVATIONS") -> {
                    currentSection = 2
                    continue
                }
                upper.contains("IMPORTANT TRENDS") -> {
                    currentSection = 3
                    continue
                }
                upper.contains("ADMINISTRATIVE INSIGHTS") -> {
                    currentSection = 4
                    continue
                }
            }

            val cleanItem = trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim()
            if (cleanItem.isEmpty()) continue

            when (currentSection) {
                1 -> {
                    execSummary = if (execSummary.isEmpty()) cleanItem else "$execSummary $cleanItem"
                }
                2 -> observations.add(cleanItem)
                3 -> trends.add(cleanItem)
                4 -> insights.add(cleanItem)
            }
        }

        if (execSummary.isBlank()) {
            execSummary = "During the ${stats.periodLabel}, ${stats.totalRequests} outpass requests were processed for the ${stats.department} department. ${stats.approvedCount} were approved and ${stats.pendingCount} remain pending."
        }

        if (observations.isEmpty()) {
            observations.add("Peak student departures occurred during ${stats.peakExitHours}.")
            if (stats.lateReturnsCount > 0) {
                observations.add("${stats.lateReturnsCount} student late returns were documented.")
            } else {
                observations.add("All students returned on schedule with zero late returns recorded.")
            }
            if (stats.topReasons.isNotEmpty()) {
                observations.add("Leading reason for outpass requests was ${stats.topReasons.first().first}.")
            }
        }

        if (trends.isEmpty()) {
            if (stats.averageApprovalTimeMinutes != null) {
                trends.add("Average faculty approval turnaround time was ${stats.averageApprovalTimeMinutes} minutes.")
            }
            trends.add("${stats.currentlyOutsideCount} students are currently active outside campus.")
        }

        if (insights.isEmpty()) {
            if (stats.pendingCount > 0) {
                insights.add("Staff Advisors and HOD should review the ${stats.pendingCount} pending applications promptly.")
            }
            if (stats.lateReturnsCount > 0) {
                insights.add("Hostel wardens and security officers should follow up on late return cases.")
            }
        }

        return AiAnalysisResult(
            executiveSummary = execSummary,
            keyObservations = observations,
            importantTrends = trends,
            administrativeInsights = insights,
            isAiGenerated = true,
            fallbackWarning = null
        )
    }

    private fun generateLocalSummary(stats: OutpassStatistics, fallbackWarning: String?): AiAnalysisResult {
        val total = stats.totalRequests
        val approved = stats.approvedCount
        val pending = stats.pendingCount
        val late = stats.lateReturnsCount
        val outside = stats.currentlyOutsideCount

        val execSummary = if (total == 0) {
            "No outpass requests were submitted for the ${stats.department} department during ${stats.periodLabel}."
        } else {
            "During ${stats.periodLabel}, a total of $total outpass requests were recorded for the ${stats.department} department. " +
                    "$approved requests were approved, while $pending requests currently remain in the review queue."
        }

        val observations = mutableListOf<String>()
        if (total > 0) {
            observations.add("Peak student departure window was recorded around ${stats.peakExitHours}.")
            if (late > 0) {
                observations.add("$late student return delay(s) were flagged by the gate verification system.")
            } else {
                observations.add("100% on-time return compliance with zero overdue occurrences recorded.")
            }
            if (stats.topReasons.isNotEmpty()) {
                val top = stats.topReasons.first()
                observations.add("Most frequent outpass reason: ${top.first} (${top.second} request${if (top.second > 1) "s" else ""}).")
            }
            if (stats.averageApprovalTimeMinutes != null) {
                observations.add("Average faculty review duration was ${stats.averageApprovalTimeMinutes} minutes.")
            }
        } else {
            observations.add("No outpass activities or student departures were logged for this selected period.")
        }

        val trends = mutableListOf<String>()
        if (total > 0) {
            if (outside > 0) {
                trends.add("$outside student(s) currently remain outside campus in active transit.")
            }
            val homeCount = stats.typeDistribution.entries.firstOrNull { it.key.name == "HOME" }?.value ?: 0
            val localCount = stats.typeDistribution.entries.firstOrNull { it.key.name == "LOCAL" }?.value ?: 0
            trends.add("Movement breakdown: $localCount Local passes vs $homeCount Home Leave passes.")
        }

        val insights = mutableListOf<String>()
        if (pending > 0) {
            insights.add("There are $pending pending outpass applications requiring immediate review by Staff Advisor / HOD.")
        }
        if (late > 0) {
            insights.add("Follow up with hostel caretakers regarding $late late-return student records.")
        } else if (total > 0) {
            insights.add("Gate compliance standards remain high with zero safety breaches.")
        }

        return AiAnalysisResult(
            executiveSummary = execSummary,
            keyObservations = observations,
            importantTrends = trends,
            administrativeInsights = insights,
            isAiGenerated = false,
            fallbackWarning = fallbackWarning
        )
    }
}
