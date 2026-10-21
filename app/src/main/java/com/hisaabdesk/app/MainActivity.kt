
package com.hisaabdesk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.navigation.AppNav
import com.hisaabdesk.app.ui.screens.OnboardingScreen
import com.hisaabdesk.app.ui.screens.PinLockScreen
import com.hisaabdesk.app.ui.theme.HisaabDeskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HisaabDeskTheme {
                val vm: MainViewModel = viewModel()
                val isPinSet by vm.pinManager.isPinSet.collectAsState(initial = null as Boolean?)
                var unlocked by remember { mutableStateOf(false) }
                var onboardDone by remember { mutableStateOf(false) }

                // Lock when app goes to background
                DisposableEffect(Unit) {
                    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) unlocked = false }
                    ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
                    onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
                }
                LaunchedEffect(unlocked) { if (unlocked) vm.postDueAutoRecurring() }

                val message by vm.message.collectAsState()
                val error by vm.error.collectAsState()
                // transient feedback
                LaunchedEffect(message, error) {
                    if (message != null || error != null) {
                        kotlinx.coroutines.delay(10)
                    }
                }
                when {
                    isPinSet == null -> androidx.compose.material3.CircularProgressIndicator()
                    isPinSet == false && !onboardDone -> OnboardingScreen(vm) { onboardDone = true; unlocked = true }
                    !unlocked -> PinLockScreen(vm) { unlocked = true }
                    else -> {
                        androidx.compose.foundation.layout.Column {
                            if (error != null) androidx.compose.material3.Text(error!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            else if (message != null) androidx.compose.material3.Text(message!!, color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            AppNav(vm)
                            LaunchedEffect(message, error) { if (message != null || error != null) { kotlinx.coroutines.delay(3500); vm.clearMessages() } }
                        }
                    }
                }
            }
        }
    }
}
