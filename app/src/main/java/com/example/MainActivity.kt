package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.InterventionViewModel
import com.example.ui.InterventionViewModelFactory
import com.example.ui.InterventionsApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core room setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(
            inspectorDao = database.inspectorDao(),
            interventionDao = database.interventionDao(),
            appNotificationDao = database.appNotificationDao(),
            userAccountDao = database.userAccountDao()
        )

        // ViewModel initialization
        val viewModel: InterventionViewModel by viewModels {
            InterventionViewModelFactory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InterventionsApp(viewModel = viewModel)
                }
            }
        }
    }
}
