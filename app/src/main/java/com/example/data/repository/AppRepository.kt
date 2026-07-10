package com.example.data.repository

import com.example.data.dao.InspectorDao
import com.example.data.dao.InterventionDao
import com.example.data.dao.AppNotificationDao
import com.example.data.dao.UserAccountDao
import com.example.data.model.Inspector
import com.example.data.model.Intervention
import com.example.data.model.AppNotification
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val inspectorDao: InspectorDao,
    private val interventionDao: InterventionDao,
    private val appNotificationDao: AppNotificationDao,
    private val userAccountDao: UserAccountDao
) {
    val allInspectors: Flow<List<Inspector>> = inspectorDao.getAllInspectors()
    val allInterventions: Flow<List<Intervention>> = interventionDao.getAllInterventions()
    val allNotifications: Flow<List<AppNotification>> = appNotificationDao.getAllNotifications()
    val allUserAccounts: Flow<List<UserAccount>> = userAccountDao.getAllUserAccounts()

    fun getUserAccountsByRole(role: String): Flow<List<UserAccount>> = userAccountDao.getUserAccountsByRole(role)
    suspend fun getUserAccountByEmail(email: String): UserAccount? = userAccountDao.getUserAccountByEmail(email)
    suspend fun insertUserAccount(userAccount: UserAccount) = userAccountDao.insertUserAccount(userAccount)
    suspend fun deleteUserAccount(userAccount: UserAccount) = userAccountDao.deleteUserAccount(userAccount)

    suspend fun getInspectorById(id: Int): Inspector? = inspectorDao.getInspectorById(id)
    suspend fun insertInspector(inspector: Inspector): Long = inspectorDao.insertInspector(inspector)
    suspend fun deleteInspector(inspector: Inspector) = inspectorDao.deleteInspector(inspector)
    suspend fun deleteInspectorById(id: Int) = inspectorDao.deleteInspectorById(id)

    suspend fun getInterventionById(id: Int): Intervention? = interventionDao.getInterventionById(id)
    suspend fun insertIntervention(intervention: Intervention): Long = interventionDao.insertIntervention(intervention)
    suspend fun updateIntervention(intervention: Intervention) = interventionDao.updateIntervention(intervention)
    suspend fun deleteIntervention(intervention: Intervention) = interventionDao.deleteIntervention(intervention)

    suspend fun insertNotification(notification: AppNotification) = appNotificationDao.insertNotification(notification)
    suspend fun markAllNotificationsAsRead() = appNotificationDao.markAllAsRead()
    suspend fun deleteNotificationById(id: Int) = appNotificationDao.deleteNotificationById(id)
    suspend fun clearAllNotifications() = appNotificationDao.clearAllNotifications()
}
