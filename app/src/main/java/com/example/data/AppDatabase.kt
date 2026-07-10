package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.InspectorDao
import com.example.data.dao.InterventionDao
import com.example.data.dao.AppNotificationDao
import com.example.data.dao.UserAccountDao
import com.example.data.model.Inspector
import com.example.data.model.Intervention
import com.example.data.model.AppNotification
import com.example.data.model.UserAccount

@Database(
    entities = [Inspector::class, Intervention::class, AppNotification::class, UserAccount::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectorDao(): InspectorDao
    abstract fun interventionDao(): InterventionDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "interventions_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
