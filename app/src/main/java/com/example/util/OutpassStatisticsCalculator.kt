package com.example.util

import com.example.data.models.Outpass
import com.example.data.models.OutpassStatistics
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.ReportDatePreset
import com.example.data.models.User
import com.example.data.models.UserRole
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object OutpassStatisticsCalculator {

    fun getDateRangeBounds(
        preset: ReportDatePreset,
        customStartMs: Long? = null,
        customEndMs: Long? = null
    ): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return when (preset) {
            ReportDatePreset.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            ReportDatePreset.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            ReportDatePreset.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end.coerceAtLeast(now))
            }
            ReportDatePreset.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end.coerceAtLeast(now))
            }
            ReportDatePreset.CUSTOM -> {
                val start = customStartMs ?: (now - 7 * 86400_000L)
                val end = customEndMs ?: now
                Pair(start.coerceAtMost(end), end.coerceAtLeast(start))
            }
        }
    }

    fun calculate(
        allRecords: List<Outpass>,
        preset: ReportDatePreset,
        user: User,
        customStartMs: Long? = null,
        customEndMs: Long? = null
    ): OutpassStatistics {
        val (startMs, endMs) = getDateRangeBounds(preset, customStartMs, customEndMs)
        val nowMs = System.currentTimeMillis()

        // Filter based on Department role authorization
        val deptScoped = if (user.role == UserRole.ADMIN) {
            allRecords
        } else {
            allRecords.filter { it.department.equals(user.department, ignoreCase = true) }
        }

        // Filter within selected time period
        val inPeriodRecords = deptScoped.filter { pass ->
            val eventTime = if (pass.appliedAt > 0) pass.appliedAt else pass.outDateTime
            eventTime in startMs..endMs || pass.outDateTime in startMs..endMs
        }

        val total = inPeriodRecords.size

        val approvedCount = inPeriodRecords.count {
            it.status == OutpassStatus.APPROVED ||
                    it.status == OutpassStatus.CHECKED_OUT ||
                    it.status == OutpassStatus.CHECKED_IN
        }

        val rejectedCount = inPeriodRecords.count { it.status == OutpassStatus.REJECTED }
        val pendingCount = inPeriodRecords.count {
            it.status == OutpassStatus.PENDING_STAFF || it.status == OutpassStatus.PENDING_HOD
        }
        val currentlyOutside = inPeriodRecords.count { it.status == OutpassStatus.CHECKED_OUT }
        val returnedCount = inPeriodRecords.count { it.status == OutpassStatus.CHECKED_IN }

        // Late Returns: returned late or currently checked out past return date
        val lateRecords = inPeriodRecords.filter { pass ->
            val returnedLate = pass.actualCheckInTime != null && pass.actualCheckInTime > pass.returnDateTime
            val overdueOutside = pass.status == OutpassStatus.CHECKED_OUT && nowMs > pass.returnDateTime
            returnedLate || overdueOutside
        }
        val lateReturnsCount = lateRecords.size

        // Average Approval Time in minutes
        val approvalDurations = inPeriodRecords.mapNotNull { pass ->
            val approvalTimestamp = pass.hodApproval?.timestamp ?: pass.staffApproval?.timestamp
            if (approvalTimestamp != null && pass.appliedAt > 0 && approvalTimestamp >= pass.appliedAt) {
                (approvalTimestamp - pass.appliedAt) / 60_000L
            } else null
        }
        val avgApprovalMinutes = if (approvalDurations.isNotEmpty()) {
            approvalDurations.average().toLong().coerceAtLeast(1)
        } else null

        // Average Return Delay for late returns
        val delays = lateRecords.mapNotNull { pass ->
            val referenceReturnTime = pass.actualCheckInTime ?: nowMs
            if (referenceReturnTime > pass.returnDateTime) {
                (referenceReturnTime - pass.returnDateTime) / 60_000L
            } else null
        }
        val avgDelayMinutes = if (delays.isNotEmpty()) {
            delays.average().toLong().coerceAtLeast(1)
        } else null

        // Top reasons
        val topReasons = inPeriodRecords
            .groupingBy { cleanReason(it.reason) }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(4)

        // Peak exit hours calculation
        val peakHourLabel = calculatePeakExitHours(inPeriodRecords)

        // Type distribution
        val typeDistribution = inPeriodRecords.groupingBy { it.type }.eachCount()

        val dateDf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val periodLabel = when (preset) {
            ReportDatePreset.TODAY -> "Today (${dateDf.format(Date(startMs))})"
            ReportDatePreset.YESTERDAY -> "Yesterday (${dateDf.format(Date(startMs))})"
            ReportDatePreset.THIS_WEEK -> "This Week (${dateDf.format(Date(startMs))} - ${dateDf.format(Date(endMs))})"
            ReportDatePreset.THIS_MONTH -> "This Month (${dateDf.format(Date(startMs))} - ${dateDf.format(Date(endMs))})"
            ReportDatePreset.CUSTOM -> "${dateDf.format(Date(startMs))} - ${dateDf.format(Date(endMs))}"
        }

        return OutpassStatistics(
            periodLabel = periodLabel,
            preset = preset,
            startTimeMs = startMs,
            endTimeMs = endMs,
            department = user.department,
            totalRequests = total,
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            pendingCount = pendingCount,
            currentlyOutsideCount = currentlyOutside,
            returnedCount = returnedCount,
            lateReturnsCount = lateReturnsCount,
            averageApprovalTimeMinutes = avgApprovalMinutes,
            averageReturnDelayMinutes = avgDelayMinutes,
            topReasons = topReasons,
            peakExitHours = peakHourLabel,
            typeDistribution = typeDistribution,
            recordsCount = total
        )
    }

    private fun cleanReason(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "General Personal Work"
        val lower = trimmed.lowercase()
        return when {
            lower.contains("medic") || lower.contains("doctor") || lower.contains("hospital") || lower.contains("blood") -> "Medical & Health Checkup"
            lower.contains("wedding") || lower.contains("marriage") || lower.contains("family") || lower.contains("home") -> "Family Event / Home Visit"
            lower.contains("book") || lower.contains("project") || lower.contains("exam") || lower.contains("hackathon") || lower.contains("study") -> "Academic / Project Work"
            lower.contains("mall") || lower.contains("market") || lower.contains("shopping") || lower.contains("store") -> "Local Shopping / Essentials"
            else -> trimmed.take(30)
        }
    }

    private fun calculatePeakExitHours(records: List<Outpass>): String {
        if (records.isEmpty()) return "N/A"
        val calendar = Calendar.getInstance()
        val hourBuckets = mutableMapOf(
            "08:00 AM - 10:00 AM" to 0,
            "10:00 AM - 12:00 PM" to 0,
            "12:00 PM - 02:00 PM" to 0,
            "02:00 PM - 04:00 PM" to 0,
            "04:00 PM - 06:00 PM" to 0,
            "06:00 PM - 08:00 PM" to 0
        )

        for (pass in records) {
            calendar.timeInMillis = pass.outDateTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val bucket = when (hour) {
                in 8..9 -> "08:00 AM - 10:00 AM"
                in 10..11 -> "10:00 AM - 12:00 PM"
                in 12..13 -> "12:00 PM - 02:00 PM"
                in 14..15 -> "02:00 PM - 04:00 PM"
                in 16..17 -> "04:00 PM - 06:00 PM"
                in 18..19 -> "06:00 PM - 08:00 PM"
                else -> "04:00 PM - 06:00 PM"
            }
            hourBuckets[bucket] = (hourBuckets[bucket] ?: 0) + 1
        }

        val topBucket = hourBuckets.maxByOrNull { it.value }
        return if (topBucket != null && topBucket.value > 0) {
            topBucket.key
        } else {
            "10:00 AM - 12:00 PM"
        }
    }
}
