package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.data.NetworkMonitor
import com.example.data.firebase.SecurityRepository
import com.example.notifications.SecurityNotificationManager
import com.example.ui.screens.MainScreen
import com.example.ui.theme.SecurityBpsTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.SecurityViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private var requestedTabState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedTabState = intent?.getStringExtra(SecurityNotificationManager.EXTRA_OPEN_TAB)
        enableEdgeToEdge()
        setContent {
            SecurityBpsTheme {
                AppNavigation(
                    requestedTab = requestedTabState,
                    onConsumeRequestedTab = { requestedTabState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTabState = intent.getStringExtra(SecurityNotificationManager.EXTRA_OPEN_TAB)
    }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    requestedTab: String? = null,
    onConsumeRequestedTab: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val context = LocalContext.current
        val networkMonitor = remember(context) { NetworkMonitor(context.applicationContext) }
        DisposableEffect(networkMonitor) {
            onDispose { networkMonitor.release() }
        }

        key(uid) {
            val viewModel: SecurityViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        SecurityViewModel(
                            repository = SecurityRepository(db),
                            networkMonitor = networkMonitor,
                            currentUserId = uid,
                            initialDisplayName = user.displayName
                        )
                    }
                }
            )
            LaunchedEffect(requestedTab) {
                when (requestedTab) {
                    "CHAT" -> {
                        viewModel.switchTab(MainTab.CHAT)
                        onConsumeRequestedTab()
                    }
                    "LAPORAN" -> {
                        viewModel.switchTab(MainTab.LAPORAN)
                        onConsumeRequestedTab()
                    }
                }
            }
            MainScreen(
                viewModel = viewModel,
                currentUserId = uid
            )
        }
    }
}
