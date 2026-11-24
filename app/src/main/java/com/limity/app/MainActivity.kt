package com.limity.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101512)
private val SurfaceDark = Color(0xFF1B2821)
private val Muted = Color(0xFFB5C2B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("limity_settings", MODE_PRIVATE)
        setContent {
            var dailyTokens by remember { mutableFloatStateOf(prefs.getInt("daily_tokens", 120).toFloat()) }
            var accentIndex by remember { mutableIntStateOf(prefs.getInt("accent_index", 0)) }
            var tab by remember { mutableIntStateOf(0) }
            val accents = listOf(Color(0xFF83C99D), Color(0xFF6BA9E8), Color(0xFFA78BFA), Color(0xFFF0A36B), Color(0xFFE88BA5), Color(0xFF55C8BE))
            val accent = accents[accentIndex.coerceIn(accents.indices)]
            MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = Background, surface = SurfaceDark, onSurface = Color.White, secondary = Muted)) {
                Scaffold(
                    containerColor = Background,
                    bottomBar = {
                        NavigationBar(containerColor = SurfaceDark) {
                            val items = listOf("Home", "Apps", "Insights", "Settings")
                            val icons = listOf(Icons.Default.Home, Icons.Default.Apps, Icons.Default.Insights, Icons.Default.Settings)
                            items.forEachIndexed { index, label ->
                                NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icons[index], contentDescription = label) }, label = { Text(label) })
                            }
                        }
                    }
                ) { padding ->
                    when (tab) {
                        0 -> HomeScreen(dailyTokens.toInt(), accent, Modifier.padding(padding))
                        1 -> AppsScreen(Modifier.padding(padding)) { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        2 -> InsightsScreen(Modifier.padding(padding))
                        else -> SettingsScreen(
                            dailyTokens, accentIndex, accents,
                            onTokens = { dailyTokens = it; prefs.edit().putInt("daily_tokens", it.toInt()).apply() },
                            onAccent = { accentIndex = it; prefs.edit().putInt("accent_index", it).apply() },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().background(Background).statusBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}

@Composable
private fun HomeScreen(tokens: Int, accent: Color, modifier: Modifier = Modifier) {
    Page(modifier) {
        Column {
            Text("limity.", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("MAKE TIME COUNT.", fontSize = 10.sp, letterSpacing = 2.sp, color = Muted)
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("YOUR DAILY ALLOWANCE", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                Text("$tokens tokens", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = accent, trackColor = Color(0xFF35473B))
                Text("Your allowance is ready. Usage tracking is coming next.", color = Muted, fontSize = 12.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("A moment for yourself.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Text("You are more than your feed. Pause, breathe, and choose what deserves your time.", color = Muted, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Air, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Pause. Breathe. Reflect.", color = accent, fontSize = 13.sp)
                }
            }
        }
        Text("Make room for what matters.", color = Color.White, fontSize = 22.sp)
    }
}

@Composable
private fun AppsScreen(modifier: Modifier = Modifier, onUsageSettings: () -> Unit) {
    Page(modifier) {
        Text("Your apps", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Choose which apps Limity should manage. App selection and live tracking are coming next.", color = Muted)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Security, tint = Color(0xFF83C99D), contentDescription = null)
                Text("Usage access", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Android requires your permission before Limity can read app usage.", color = Muted, fontSize = 13.sp)
                Button(onClick = onUsageSettings) { Text("Open usage access settings") }
            }
        }
    }
}

@Composable
private fun InsightsScreen(modifier: Modifier = Modifier) {
    Page(modifier) {
        Text("Your insights", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Your screen-time story will appear here once usage tracking is implemented.", color = Muted)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
            Column(Modifier.padding(18.dp)) {
                Icon(Icons.Default.AutoGraph, tint = Color(0xFF83C99D), contentDescription = null)
                Spacer(Modifier.height(10.dp))
                Text("Small steps add up.", color = Color.White, fontSize = 19.sp)
                Text("Build a healthier relationship with your time.", color = Muted)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    dailyTokens: Float, accentIndex: Int, accents: List<Color>,
    onTokens: (Float) -> Unit, onAccent: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val names = listOf("Sage", "Blue", "Purple", "Orange", "Rose", "Teal")
    Page(modifier) {
        Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DAILY TOKEN ALLOWANCE", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
                Text("${dailyTokens.toInt()} tokens", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Slider(value = dailyTokens, onValueChange = onTokens, valueRange = 15f..600f, steps = 38)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("15", color = Muted, fontSize = 11.sp)
                    Text("600", color = Muted, fontSize = 11.sp)
                }
                Text("1 token = 1 minute of selected app usage.", color = Muted, fontSize = 12.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("APP ACCENT COLOUR", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
                Text("Make Limity yours.", color = Color.White, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    accents.forEachIndexed { index, color ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(color).clickable { onAccent(index) }, contentAlignment = Alignment.Center) {
                                if (accentIndex == index) Icon(Icons.Default.Check, contentDescription = "Selected", tint = Background)
                            }
                            Text(names[index], color = Muted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
        Text("App selection, milestone reminders, reset schedule and blocking are on the roadmap.", color = Muted, fontSize = 12.sp)
    }
}
