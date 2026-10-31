package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.models.Outpass
import com.example.data.models.User
import com.example.data.models.UserRole
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateAndSavePdf(
        context: Context,
        user: User,
        records: List<Outpass>,
        isClassReport: Boolean
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            // Landscape A4 for wide, perfect column alignment
            val pageWidth = 842
            val pageHeight = 595
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42) // Slate 900
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(16, 185, 129) // Emerald Green
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val metaPaint = Paint().apply {
                color = Color.rgb(71, 85, 105) // Slate 600
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val boldBodyPaint = Paint().apply {
                color = Color.rgb(15, 23, 42) // Slate 900
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val regularBodyPaint = Paint().apply {
                color = Color.rgb(51, 65, 85) // Slate 700
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249) // Slate 100
            }

            val zebraBgPaint = Paint().apply {
                color = Color.rgb(248, 250, 252) // Slate 50
            }

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
            val timingDf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

            var y = 35f

            // Document Header
            val reportTitle = if (isClassReport) "CLASS ADVISOR STUDENT OUTPASS STATEMENT" else "HOD DEPARTMENT OUTPASS AUDIT REPORT"
            canvas.drawText("VETIAS CAMPUS DIGITAL OUTPASS SYSTEM", 30f, y, titlePaint)
            y += 20f
            canvas.drawText(reportTitle, 30f, y, subtitlePaint)
            y += 18f

            val roleTitle = if (user.role == UserRole.STAFF_ADVISOR) "Class Advisor" else if (user.role == UserRole.HOD) "Head of Department" else "Admin"
            val scopeText = "$roleTitle: ${user.name} | Department: ${user.department}"
            canvas.drawText("$scopeText | Generated: $dateStr | Total Records: ${records.size}", 30f, y, metaPaint)
            y += 14f

            canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
            y += 12f

            // Table Header definition with exact column alignments
            val colSl = 32f
            val colPassId = 65f
            val colStudent = 145f
            val colRegNo = 275f
            val colTiming = 360f
            val colReason = 530f
            val colStatus = 705f

            fun drawTableHeader(curY: Float) {
                canvas.drawRect(30f, curY, pageWidth - 30f, curY + 24f, headerBgPaint)
                val hY = curY + 16f
                canvas.drawText("#", colSl, hY, boldBodyPaint)
                canvas.drawText("PASS ID", colPassId, hY, boldBodyPaint)
                canvas.drawText("STUDENT NAME", colStudent, hY, boldBodyPaint)
                canvas.drawText("REGISTER NO", colRegNo, hY, boldBodyPaint)
                canvas.drawText("OUTPASS TIMING", colTiming, hY, boldBodyPaint)
                canvas.drawText("REASON", colReason, hY, boldBodyPaint)
                canvas.drawText("TYPE & STATUS", colStatus, hY, boldBodyPaint)
                canvas.drawLine(30f, curY + 24f, pageWidth - 30f, curY + 24f, linePaint)
            }

            drawTableHeader(y)
            y += 28f

            // Rows
            if (records.isEmpty()) {
                canvas.drawText("No outpass records found for the selected department/period.", 40f, y + 14f, regularBodyPaint)
                y += 28f
            } else {
                for ((index, outpass) in records.withIndex()) {
                    if (y > pageHeight - 65) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 35f
                        drawTableHeader(y)
                        y += 28f
                    }

                    // Zebra background for clean readability
                    if (index % 2 == 1) {
                        canvas.drawRect(30f, y - 4f, pageWidth - 30f, y + 20f, zebraBgPaint)
                    }

                    val rowY = y + 12f

                    // SL
                    canvas.drawText("${index + 1}", colSl, rowY, regularBodyPaint)

                    // PASS ID
                    canvas.drawText(outpass.id, colPassId, rowY, boldBodyPaint)

                    // STUDENT NAME
                    val sName = if (outpass.studentName.length > 20) outpass.studentName.take(18) + ".." else outpass.studentName
                    canvas.drawText(sName, colStudent, rowY, boldBodyPaint)

                    // REGISTER NO
                    canvas.drawText(outpass.regNo, colRegNo, rowY, regularBodyPaint)

                    // OUTPASS TIMING (Formatted cleanly: e.g. "23 Oct, 09:30 AM → 23 Oct, 02:00 PM")
                    val outTimeStr = timingDf.format(Date(outpass.outDateTime))
                    val retTimeStr = timingDf.format(Date(outpass.returnDateTime))
                    val timingStr = "$outTimeStr → $retTimeStr"
                    canvas.drawText(timingStr, colTiming, rowY, regularBodyPaint)

                    // REASON
                    val cleanReason = if (outpass.reason.length > 28) outpass.reason.take(26) + ".." else outpass.reason
                    canvas.drawText(cleanReason, colReason, rowY, regularBodyPaint)

                    // TYPE & STATUS
                    val statusText = "${outpass.type.name} • ${outpass.status.displayName}"
                    val statusPaint = if (outpass.status.name.contains("APPROVED") || outpass.status.name.contains("CHECKED_IN")) {
                        Paint(boldBodyPaint).apply { color = Color.rgb(16, 185, 129) }
                    } else if (outpass.status.name.contains("REJECTED")) {
                        Paint(boldBodyPaint).apply { color = Color.rgb(239, 68, 68) }
                    } else {
                        boldBodyPaint
                    }
                    canvas.drawText(statusText, colStatus, rowY, statusPaint)

                    y += 22f
                    canvas.drawLine(30f, y - 2f, pageWidth - 30f, y - 2f, linePaint)
                }
            }

            // Summary Footer
            y += 14f
            if (y > pageHeight - 65) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 35f
            }

            canvas.drawRect(30f, y, pageWidth - 30f, y + 36f, headerBgPaint)
            val footerY = y + 15f
            canvas.drawText("Official Outpass Report • Verified by VETIAS Campus Security & Academic Administration", 40f, footerY, boldBodyPaint)
            canvas.drawText("Contains Student Name, Register Number, Approved Timing, and Stated Departure Reason for Official Audit.", 40f, footerY + 14f, metaPaint)

            pdfDocument.finishPage(page)

            // Save File to Downloads
            val deptClean = user.department.replace(" ", "_").replace("&", "And")
            val fileName = if (isClassReport) {
                "Class_Outpass_Report_${deptClean}_${System.currentTimeMillis() % 10000}.pdf"
            } else {
                "HOD_Dept_Outpass_Report_${deptClean}_${System.currentTimeMillis() % 10000}.pdf"
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(downloadsDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun openOrSharePdf(context: Context, file: File, isShare: Boolean = false) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(if (isShare) Intent.ACTION_SEND else Intent.ACTION_VIEW).apply {
                if (isShare) {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Outpass PDF Report: ${file.name}")
                } else {
                    setDataAndType(uri, "application/pdf")
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, if (isShare) "Share Outpass PDF Report" else "Open Downloaded Outpass Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Report saved: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }
}
