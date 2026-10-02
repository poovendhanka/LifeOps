package com.lifeops

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeops.core.design.*
import com.lifeops.feature.*

class MainActivity:ComponentActivity() {
 private var captureRequested by mutableStateOf(false)
 private var openedId by mutableStateOf<String?>(null)
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
  // Compose restores navigation and editor state on recreation. Replaying a
  // shortcut/notification here would open its destination a second time.
  if(savedInstanceState==null){openedId=intent.getStringExtra("itemId");captureRequested=intent.getBooleanExtra("capture",false)}
  setContent {LifeOpsTheme {val vm:LifeViewModel=viewModel(); LifeOpsRoot(vm,openedId,captureRequested){openedId=null;captureRequested=false}}}
 }
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);openedId=intent.getStringExtra("itemId");captureRequested=intent.getBooleanExtra("capture",false)}
}
