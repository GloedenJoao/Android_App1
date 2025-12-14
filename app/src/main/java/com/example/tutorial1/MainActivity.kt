package com.example.tutorial1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import com.example.tutorial1.data.FinanceDatabase
import com.example.tutorial1.data.FinanceRepository
import com.example.tutorial1.ui.FinanceApp
import com.example.tutorial1.ui.FinanceViewModel
import com.example.tutorial1.ui.theme.Tutorial1Theme

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels {
        val db = FinanceDatabase.getInstance(applicationContext)
        val repository = FinanceRepository(db.financeDao())
        FinanceViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Tutorial1Theme {
                Surface {
                    FinanceApp(viewModel)
                }
            }
        }
    }
}
