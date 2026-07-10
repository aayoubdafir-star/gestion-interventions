package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspectors")
data class Inspector(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val specialty: String
) {
    override fun toString(): String = name
}

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val name: String,
    val role: String, // "HEAD", "CHEF_DE_PROJET", "INSPECTEUR"
    val phone: String = "",
    val specialty: String = ""
)

@Entity(tableName = "interventions")
data class Intervention(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientName: String,
    val clientContactEmail: String,
    val clientContactPhone: String,
    val interventionType: String,
    val factory: String,
    val zone: String,
    val equipment: String,
    val assignedInspectorIdsCsv: String, // Comma separated inspector IDs
    val status: String = "CREE", // "CREE", "ACCEPTE", "EN_COURS", "PAS_FAIT", "TERMINE"
    val remarkIfNotDone: String? = null,
    val reportStatus: String = "AUCUN", // "AUCUN", "EN_COURS_REDACTION", "FAIT"
    val reportValidated: Boolean = false,
    val reportSentToClient: Boolean = false,
    val isClosed: Boolean = false,
    val closeMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastStatusUpdateTime: Long = System.currentTimeMillis(),
    val lastReportUpdateTime: Long = System.currentTimeMillis()
) {
    val assignedInspectorIds: List<Int>
        get() = if (assignedInspectorIdsCsv.isBlank()) emptyList() 
                else assignedInspectorIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
}

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipientRole: String, // "CHEF_DE_PROJET", "HEAD", "INSPECTEUR"
    val recipientInspectorId: Int? = null, // if specific inspector
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
