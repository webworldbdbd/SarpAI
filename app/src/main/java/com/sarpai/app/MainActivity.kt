package com.sarpai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarpai.app.ui.AppRoot
import com.sarpai.app.ui.AppViewModel
import com.sarpai.app.ui.SarpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val theme by vm.theme.collectAsState()
            SarpTheme(theme) { AppRoot(vm) }
        }
    }
}
