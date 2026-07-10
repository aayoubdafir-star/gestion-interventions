package com.example.data.dao

import androidx.room.*
import com.example.data.model.Inspector
import com.example.data.model.Intervention
import com.example.data.model.AppNotification
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectorDao {
    @Query("SELECT * FROM inspectors ORDER BY id DESC")
    fun getAllInspectors(): Flow<List<Inspector>>

    @Query("SELECT * FROM inspectors WHERE id = :id")
    suspend fun getInspectorById(id: Int): Inspector?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspector(inspector: Inspector): Long

    @Delete
    suspend fun deleteInspector(inspector: Inspector)

    @Query("DELETE FROM inspectors WHERE id = :id")
    suspend fun deleteInspectorById(id: Int)
}

@Dao
interface InterventionDao {
    @Query("SELECT * FROM interventions ORDER BY createdAt DESC")
    fun getAllInterventions(): Flow<List<Intervention>>

    @Query("SELECT * FROM interventions WHERE id = :id")
    suspend fun getInterventionById(id: Int): Intervention?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: Intervention): Long

    @Update
    suspend fun updateIntervention(intervention: Intervention)

    @Delete
    suspend fun deleteIntervention(intervention: Intervention)
}

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)

    @Query("DELETE FROM app_notifications")
    suspend fun clearAllNotifications()
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts ORDER BY email ASC")
    fun getAllUserAccounts(): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_accounts WHERE role = :role ORDER BY email ASC")
    fun getUserAccountsByRole(role: String): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccountByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(userAccount: UserAccount)

    @Delete
    suspend fun deleteUserAccount(userAccount: UserAccount)
}
