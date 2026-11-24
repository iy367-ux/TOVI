package com.limity.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val Bg = Color(0xFF101512)
private val Panel = Color(0xFF1B2821)
private val Panel2 = Color(0xFF26352C)
private val Ink = Color(0xFFF2F4EF)
private val Muted = Color(0xFFB5C2B8)
private data class AppEntry(val pkg: String, val name: String)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("tovi_settings", MODE_PRIVATE) }
    private fun serviceOn(): Boolean {
        val expected = ComponentName(this, ToviAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(expected, true) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apps = packageManager.getInstalledApplications(0).mapNotNull {
            val launchable = packageManager.getLaunchIntentForPackage(it.packageName) != null
            if (!launchable || it.packageName == packageName) null
            else AppEntry(it.packageName, packageManager.getApplicationLabel(it).toString())
        }.distinctBy { it.pkg }.sortedBy { it.name.lowercase() }
        setContent {
            var setup by remember { mutableStateOf(!prefs.getBoolean("setup_done", false)) }
            var enabled by remember { mutableStateOf(serviceOn()) }
            var chosen by remember { mutableStateOf(prefs.getStringSet("selected_apps", emptySet())?.toSet() ?: emptySet()) }
            var limit by remember { mutableFloatStateOf(prefs.getInt("daily_tokens", 120).toFloat()) }
            var accentIndex by remember { mutableIntStateOf(prefs.getInt("accent_index", 0)) }
            var tab by remember { mutableIntStateOf(0) }
            val accents = listOf(Color(0xFF83C99D), Color(0xFF6BA9E8), Color(0xFFA78BFA), Color(0xFFF0A36B), Color(0xFFE88BA5), Color(0xFF55C8BE))
            val accent = accents[accentIndex.coerceIn(0, accents.lastIndex)]
            MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = Bg, surface = Panel, onSurface = Ink, secondary = Muted)) {
                if (setup) {
                    SetupFlow(
                        apps = apps, chosen = chosen, onToggle = { pkg -> chosen = if (pkg in chosen) chosen - pkg else chosen + pkg },
                        limit = limit, onLimit = { limit = it }, serviceEnabled = enabled,
                        onEnable = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onCheck = { enabled = serviceOn() },
                        onFinish = {
                            enabled = serviceOn()
                            if (enabled && chosen.isNotEmpty()) {
                                prefs.edit().putBoolean("setup_done", true).putStringSet("selected_apps", chosen)
                                    .putInt("daily_tokens", limit.toInt()).putInt("tokens_remaining", limit.toInt()).apply()
                                setup = false
                            }
                        }
                    )
                } else {
                    Scaffold(containerColor = Bg, bottomBar = {
                        NavigationBar(containerColor = Panel) {
                            val labels = listOf("Home", "My apps", "Insights", "Settings")
                            val icons = listOf(Icons.Default.Home, Icons.Default.Apps, Icons.Default.Insights, Icons.Default.Settings)
                            labels.forEachIndexed { i, label ->
                                NavigationBarItem(selected = tab == i, onClick = { tab = i },
                                    icon = { Icon(icons[i], contentDescription = label) }, label = { Text(label) })
                            }
                        }
                    }) { pad ->
                        when (tab) {
                            0 -> HomePage(limit.toInt(), accent, apps.filter { it.pkg in chosen }, Modifier.padding(pad))
                            1 -> AppsPage(apps.filter { it.pkg in chosen }, { setup = true }, Modifier.padding(pad))
                            2 -> SimplePage("Your insights", "Your TOVI journey starts here. As you use the app, your screen-time patterns and progress can build here.", Modifier.padding(pad))
                            else -> SettingsPage(limit, accentIndex, accents, { limit = it; prefs.edit().putInt("daily_tokens", it.toInt()).apply() },
                                { accentIndex = it; prefs.edit().putInt("accent_index", it).apply() }, Modifier.padding(pad))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupFlow(
    apps: List<AppEntry>, chosen: Set<String>, onToggle: (String) -> Unit,
    limit: Float, onLimit: (Float) -> Unit, serviceEnabled: Boolean,
    onEnable: () -> Unit, onCheck: () -> Unit, onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)) {
        Text("tovi.", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text("TIME, ON YOUR TERMS.", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i -> Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i <= step) Color(0xFF83C99D) else Panel2)) }
        }
        when (step) {
            0 -> {
                Text("A better balance\nstarts here.", color = Ink, fontSize = 31.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
                Text("You choose the apps. You choose your daily allowance. When you open a selected app, TOVI creates a moment to pause and decide if that's how you want to spend your time.", color = Muted, fontSize = 15.sp, lineHeight = 23.sp)
                SetupCard("Your time, your rules", "One token represents one minute of selected-app use.")
                SetupCard("A pause, not a punishment", "TOVI reminds you what you intended, then lets you choose what happens next.")
                SetupCard("Private by design", "Your app list and preferences stay on this device.")
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Let's set it up  →") }
            }
            1 -> {
                Text("What distracts\nyou most?", color = Ink, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                Text("Select the apps where you'd like TOVI to step in. You can change this list later.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
                Text("${chosen.size} apps selected", color = Color(0xFF83C99D), fontWeight = FontWeight.SemiBold)
                Column(Modifier.heightIn(max = 390.dp).clip(RoundedCornerShape(18.dp)).background(Panel).verticalScroll(rememberScrollState())) {
                    apps.forEach { app ->
                        Row(Modifier.fillMaxWidth().clickable { onToggle(app.pkg) }.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(app.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(app.pkg, color = Muted, fontSize = 9.sp)
                            }
                            Checkbox(checked = app.pkg in chosen, onCheckedChange = { onToggle(app.pkg) })
                        }
                        HorizontalDivider(color = Ink.copy(alpha = .06f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Back") }
                    Button(onClick = { step = 2 }, enabled = chosen.isNotEmpty(), modifier = Modifier.weight(1f).height(52.dp)) { Text("Continue") }
                }
            }
            else -> {
                Text("Make it yours.", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Set your daily allowance, then enable TOVI's intervention service. Android needs your permission before TOVI can notice when a selected app opens.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
                SetupCard("Your daily allowance", "1 token = 1 minute in an app you selected.")
                Text("${limit.toInt()} tokens per day", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Slider(value = limit, onValueChange = onLimit, valueRange = 15f..600f, steps = 38)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("15 min", color = Muted, fontSize = 11.sp); Text("10 hours", color = Muted, fontSize = 11.sp)
                }
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enable app interventions", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("TOVI uses Android Accessibility only to detect which app is in the foreground and show its pause screen. It does not inspect messages, photos or what you type.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) { Text("Open Accessibility settings") }
                        OutlinedButton(onClick = onCheck, modifier = Modifier.fillMaxWidth()) { Text("I've enabled it — check again") }
                        Text(if (serviceEnabled) "✓ Intervention service enabled" else "Not enabled yet", color = if (serviceEnabled) Color(0xFF83C99D) else Color(0xFFE4B17B), fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Back") }
                    Button(onClick = onFinish, enabled = serviceEnabled && chosen.isNotEmpty(), modifier = Modifier.weight(1f).height(52.dp)) { Text("Start TOVI  →") }
                }
            }
        }
    }
}

@Composable
private fun SetupCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(body, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().background(Bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp), content = content)
}

@Composable
private fun HomePage(tokens: Int, accent: Color, apps: List<AppEntry>, modifier: Modifier = Modifier) {
    Page(modifier) {
        Column {
            Text("Make today count.", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Your space, your pace.", color = Muted, fontSize = 13.sp)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(23.dp)) {
            Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("TIME LEFT TODAY", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$tokens", color = Ink, fontSize = 43.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp)); Text("tokens", color = accent, modifier = Modifier.padding(bottom = 7.dp))
                }
                LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = accent, trackColor = Panel2)
                Text("Your daily allowance is ready.", color = Muted, fontSize = 12.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel2), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A small pause can change your day.", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Open one of your selected apps. TOVI will step in with a reminder and give you a choice.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                Text("Pause. Breathe. Decide.", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("APPS YOU'VE CHOSEN", color = Muted, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
        apps.forEach { app ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Panel2), contentAlignment = Alignment.Center) {
                    Text(app.name.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp)); Text(app.name, color = Ink, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f)); Text("WATCHING", color = accent, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun AppsPage(apps: List<AppEntry>, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Page(modifier) {
        Text("Your apps", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("TOVI will pause you when you open these apps.", color = Muted)
        apps.forEach { app ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(app.name.take(1).uppercase(), color = Color(0xFF83C99D), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp)); Text(app.name, color = Ink, modifier = Modifier.weight(1f))
                Text("ON", color = Color(0xFF83C99D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Change selected apps") }
    }
}

@Composable
private fun SimplePage(title: String, description: String, modifier: Modifier = Modifier) {
    Page(modifier) {
        Text(title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        SetupCard("No shame. Just awareness.", "TOVI is here to help you make a choice, not judge the choices you make.")
    }
}

@Composable
private fun SettingsPage(
    limit: Float, selected: Int, accents: List<Color>, onLimit: (Float) -> Unit,
    onAccent: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val names = listOf("Sage", "Blue", "Purple", "Orange", "Rose", "Teal")
    Page(modifier) {
        Text("Settings", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        SetupCard("Daily allowance", "1 token represents one minute of selected-app use.")
        Text("${limit.toInt()} tokens per day", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Slider(value = limit, onValueChange = onLimit, valueRange = 15f..600f, steps = 38)
        Text("APP ACCENT COLOUR", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            accents.forEachIndexed { i, color ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(color).clickable { onAccent(i) })
                    Text(names[i], color = Muted, fontSize = 9.sp)
                }
            }
        }
    }
}
