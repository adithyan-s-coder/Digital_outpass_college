package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.models.GateLog

@Entity(tableName = "gate_logs")
data class GateLogEntity(
    @PrimaryKey val id: String,
    val outpassId: String,
    val studentName: String,
    val regNo: String,
    val action: String,
    val timestamp: Long,
    val officerName: String,
    val remarks: String = ""
) {
    fun toGateLog(): GateLog = GateLog(
        id = id,
        outpassId = outpassId,
        studentName = studentName,
        regNo = regNo,
        action = action,
        timestamp = timestamp,
        officerName = officerName,
        remarks = remarks
    )

    companion object {
        fun fromGateLog(log: GateLog): GateLogEntity = GateLogEntity(
            id = log.id,
            outpassId = log.outpassId,
            studentName = log.studentName,
            regNo = log.regNo,
            action = log.action,
            timestamp = log.timestamp,
            officerName = log.officerName,
            remarks = log.remarks
        )
    }
}
