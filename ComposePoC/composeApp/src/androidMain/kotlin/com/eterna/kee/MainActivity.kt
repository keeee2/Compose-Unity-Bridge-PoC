package com.eterna.kee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UnityBridge.init(this)

        setContent {
            var showUnity by remember { mutableStateOf(false) }
            if (showUnity) {
                // Unity 풀스크린
                AndroidView(
                    factory = { UnityBridge.getPlayer()!! },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Compose 화면
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ETERNA PoC", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showUnity = true }) {
                        Text("3D 캐릭터 보기")
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); UnityBridge.resume() }
    override fun onPause() { super.onPause(); UnityBridge.pause() }
    override fun onDestroy() { super.onDestroy(); UnityBridge.destroy() }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}