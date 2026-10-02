package com.example

import com.example.data.models.ApprovalRecord
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.ReportDatePreset
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.util.GeminiReportService
import com.example.util.OutpassStatisticsCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    private val now = System.currentTimeMillis()
    private val hourMs = 3600_000L

    private val staffUser = User(
        id = "u-vance",
        name = "Dr. Robert Vance",
        email = "r.vance@vetias.ac.in",
        role = UserRole.STAFF_ADVISOR,
        department = "Computer Science"
    )

    private val samplePasses = listOf(
        Outpass(
            id = "PASS-1",
            studentId = "u-alex",
            studentName = "Alex Morgan",
            regNo = "21CS045",
            department = "Computer Science",
            hostelBlock = "Block A",
            roomNo = "302",
            studentPhone = "9876543210",
            parentPhone = "9123456789",
            type = OutpassType.HOME,
            destination = "Home",
            reason = "Family function",
            outDateTime = now - 2 * hourMs,
            returnDateTime = now + 24 * hourMs,
            status = OutpassStatus.APPROVED,
            qrToken = "token1",
            appliedAt = now - 4 * hourMs,
            staffApproval = ApprovalRecord(UserRole.STAFF_ADVISOR, "Dr. Vance", "APPROVED", now - 3 * hourMs)
        ),
        Outpass(
            id = "PASS-2",
            studentId = "u-sarah",
            studentName = "Sarah Chen",
            regNo = "22EC012",
            department = "Electronics & Comm",
            hostelBlock = "Block B",
            roomNo = "105",
            studentPhone = "9876543211",
            parentPhone = "9123456788",
            type = OutpassType.LOCAL,
            destination = "Mall",
            reason = "Shopping",
            outDateTime = now - 1 * hourMs,
            returnDateTime = now + 4 * hourMs,
            status = OutpassStatus.PENDING_STAFF,
            qrToken = "token2",
            appliedAt = now - 1 * hourMs
        ),
        Outpass(
            id = "PASS-3",
            studentId = "u-john",
            studentName = "John Doe",
            regNo = "21CS099",
            department = "Computer Science",
            hostelBlock = "Block A",
            roomNo = "305",
            studentPhone = "9876543212",
            parentPhone = "9123456787",
            type = OutpassType.LOCAL,
            destination = "Hospital",
            reason = "Medical checkup",
            outDateTime = now - 6 * hourMs,
            returnDateTime = now - 2 * hourMs,
            status = OutpassStatus.CHECKED_IN,
            qrToken = "token3",
            appliedAt = now - 8 * hourMs,
            staffApproval = ApprovalRecord(UserRole.STAFF_ADVISOR, "Dr. Vance", "APPROVED", now - 7 * hourMs),
            actualCheckOutTime = now - 5 * hourMs,
            actualCheckInTime = now - 1 * hourMs // Returned 1 hour late
        ),
        Outpass(
            id = "PASS-4",
            studentId = "u-kevin",
            studentName = "Kevin Brown",
            regNo = "21CS101",
            department = "Computer Science",
            hostelBlock = "Block A",
            roomNo = "306",
            studentPhone = "9876543213",
            parentPhone = "9123456786",
            type = OutpassType.LOCAL,
            destination = "Market",
            reason = "Personal work",
            outDateTime = now - 3 * hourMs,
            returnDateTime = now + 2 * hourMs,
            status = OutpassStatus.REJECTED,
            qrToken = "token4",
            appliedAt = now - 4 * hourMs
        )
    )

    @Test
    fun testDepartmentFiltering() {
        val stats = OutpassStatisticsCalculator.calculate(
            allRecords = samplePasses,
            preset = ReportDatePreset.THIS_MONTH,
            user = staffUser
        )

        // Only Computer Science passes (PASS-1, PASS-3, PASS-4) should be included, not EC (PASS-2)
        assertEquals(3, stats.totalRequests)
        assertEquals("Computer Science", stats.department)
        assertEquals(2, stats.approvedCount) // PASS-1 (APPROVED) and PASS-3 (CHECKED_IN)
        assertEquals(1, stats.rejectedCount) // PASS-4
    }

    @Test
    fun testLateReturnCalculation() {
        val stats = OutpassStatisticsCalculator.calculate(
            allRecords = samplePasses,
            preset = ReportDatePreset.THIS_WEEK,
            user = staffUser
        )

        // PASS-3 was due at now - 2h, but returned at now - 1h -> late return!
        assertEquals(1, stats.lateReturnsCount)
        assertNotNull(stats.averageReturnDelayMinutes)
        assertTrue(stats.averageReturnDelayMinutes!! > 0)
    }

    @Test
    fun testGeminiReportServiceGracefulFallback() = runBlocking {
        val stats = OutpassStatisticsCalculator.calculate(
            allRecords = samplePasses,
            preset = ReportDatePreset.THIS_WEEK,
            user = staffUser
        )

        val analysis = GeminiReportService.analyzeStatistics(stats)
        assertNotNull(analysis.executiveSummary)
        assertTrue(analysis.executiveSummary.isNotEmpty())
        assertTrue(analysis.keyObservations.isNotEmpty())
        assertTrue(analysis.administrativeInsights.isNotEmpty())
    }

    @Test
    fun test410PmSchedulerTargetHourAndMinute() {
        val targetMillis = com.example.util.DailyHodReportScheduler.getNext410PmMillis()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = targetMillis }
        assertEquals(16, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(10, cal.get(java.util.Calendar.MINUTE))
        assertEquals(0, cal.get(java.util.Calendar.SECOND))
        assertTrue(targetMillis > System.currentTimeMillis() - 1000)
    }

    @Test
    fun test410PmEmailFormatting() {
        val stats = OutpassStatisticsCalculator.calculate(
            allRecords = samplePasses,
            preset = ReportDatePreset.TODAY,
            user = staffUser
        )
        val analysis = com.example.data.models.AiAnalysisResult(
            executiveSummary = "Department operations ran smoothly with high student compliance.",
            keyObservations = listOf("Zero unauthorized exits recorded."),
            importantTrends = listOf("Peak exit requests at 4 PM."),
            administrativeInsights = listOf("Continue routine faculty verification."),
            isAiGenerated = true
        )
        val email = com.example.util.DailyHodReportScheduler.formatEmailContent(stats, analysis, "hod.cs.vetias@gmail.com")
        assertTrue(email.contains("OFFICIAL DEPARTMENT OUTPASS DAILY REPORT"))
        assertTrue(email.contains("4:10 PM"))
        assertTrue(email.contains("hod.cs.vetias@gmail.com"))
        assertTrue(email.contains("Computer Science"))
    }
}
