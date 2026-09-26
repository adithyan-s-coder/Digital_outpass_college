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
            val pageWidth = 595
            val pageHeight = 842
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
                color = Color.rgb(79, 70, 229) // Indigo Primary
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(51, 65, 85) // Slate 700
                textSize = 10f
                isAntiAlias = true
            }

            val boldBodyPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249) // Slate 100
            }

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            var y = 40f

            // Document Header
            val reportTitle = if (isClassReport) "CLASS STUDENTS OUTPASS STATEMENT" else "DEPARTMENT STUDENTS OUTPASS STATEMENT"
            canvas.drawText("VETIAS CAMPUS DIGITAL OUTPASS SYSTEM", 30f, y, titlePaint)
            y += 22f
            canvas.drawText(reportTitle, 30f, y, subtitlePaint)
            y += 18f

            val roleTitle = if (user.role == UserRole.STAFF_ADVISOR) "Class Advisor" else "HOD"
            val scopeText = "$roleTitle: ${user.name} | Dept: ${user.department}"
            canvas.drawText("$scopeText | Generated: $dateStr | Total Records: ${records.size}", 30f, y, bodyPaint)
            y += 16f

            canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
            y += 16f

            // Table Header
            canvas.drawRect(30f, y, pageWidth - 30f, y + 24f, headerBgPaint)
            val headerY = y + 16f
            canvas.drawText("PASS ID", 35f, headerY, boldBodyPaint)
            canvas.drawText("STUDENT & REG NO", 110f, headerY, boldBodyPaint)
            canvas.drawText("TYPE", 240f, headerY, boldBodyPaint)
            canvas.drawText("DESTINATION", 305f, headerY, boldBodyPaint)
            canvas.drawText("STATUS", 435f, headerY, boldBodyPaint)
            canvas.drawText("OUT DATE", 510f, headerY, boldBodyPaint)
            y += 30f

            // Rows
            if (records.isEmpty()) {
                canvas.drawText("No outpass records found for this export scope.", 35f, y + 12f, bodyPaint)
                y += 24f
            } else {
                for (outpass in records) {
                    if (y > pageHeight - 70) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 40f
                    }

                    val rowY = y + 12f
                    canvas.drawText(outpass.id, 35f, rowY, boldBodyPaint)
                    val studentDesc = if (outpass.studentName.length > 17) outpass.studentName.take(15) + ".." else outpass.studentName
                    canvas.drawText("$studentDesc (${outpass.regNo})", 110f, rowY, bodyPaint)
                    canvas.drawText(outpass.type.name, 240f, rowY, bodyPaint)
                    val destDesc = if (outpass.destination.length > 18) outpass.destination.take(16) + ".." else outpass.destination
                    canvas.drawText(destDesc, 305f, rowY, bodyPaint)
                    canvas.drawText(outpass.status.displayName, 435f, rowY, boldBodyPaint)
                    val outDateShort = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(outpass.outDateTime))
                    canvas.drawText(outDateShort, 510f, rowY, bodyPaint)

                    y += 22f
                    canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
                    y += 4f
                }
            }

            // Summary Footer
            y += 20f
            if (y > pageHeight - 80) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            canvas.drawRect(30f, y, pageWidth - 30f, y + 42f, headerBgPaint)
            val footerY = y + 18f
            canvas.drawText("Official Document Verified by VETIAS Campus Security & Administration", 40f, footerY, boldBodyPaint)
            canvas.drawText("Note: This is an official digital statement generated for academic audit purposes.", 40f, footerY + 14f, bodyPaint)

            pdfDocument.finishPage(page)

            // Save File
            val fileName = if (isClassReport) {
                "Class_Outpass_Records_${user.department.replace(" ", "_")}.pdf"
            } else {
                "HOD_Dept_Outpass_Records_${user.department.replace(" ", "_")}.pdf"
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
                    putExtra(Intent.EXTRA_SUBJECT, "Outpass PDF Export: ${file.name}")
                } else {
                    setDataAndType(uri, "application/pdf")
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, if (isShare) "Share Outpass PDF" else "Open Outpass PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Saved PDF to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}
