package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TaskDB
import com.example.data.TaskRepository
import com.example.ui.TaskApp
import com.example.ui.TaskViewModel
import com.example.ui.TaskViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val db = TaskDB.get(applicationContext)
                val repo = TaskRepository(db.dao())
                val factory = TaskViewModelFactory(application, repo)
                val viewModel: TaskViewModel = viewModel(factory = factory)

                TaskApp(viewModel = viewModel)
            }
        }
    }
}
