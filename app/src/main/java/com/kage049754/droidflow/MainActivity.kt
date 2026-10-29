package com.kage049754.droidflow

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class FlowRule(val id: Long, val name: String, val trigger: String, val action: String, val enabled: Boolean)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { DroidFlowApp() } }
}

private val seedRules = listOf(
 FlowRule(1, "Study focus", "Manual", "Open Do Not Disturb settings", true),
 FlowRule(2, "Battery saver", "Manual", "Open battery settings", false),
 FlowRule(3, "Night routine", "Manual", "Open display settings", false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidFlowApp() {
 val context = LocalContext.current
 val prefs = remember { context.getSharedPreferences("droidflow", 0) }
 var rules by remember {
  mutableStateOf(run {
   val raw = prefs.getString("rules", null)
   if (raw.isNullOrBlank()) seedRules else try {
    val a = JSONArray(raw)
    (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
     FlowRule(o.getLong("id"), o.getString("name"), o.getString("trigger"), o.getString("action"), o.getBoolean("enabled"))
    } }
   } catch (_: Exception) { seedRules }
  })
 }
 var tab by remember { mutableIntStateOf(0) }
 var showAdd by remember { mutableStateOf(false) }
 var newName by remember { mutableStateOf("") }
 var newAction by remember { mutableStateOf("Open Wi-Fi settings") }
 var message by remember { mutableStateOf("Flows run only when you choose them.") }

 fun save(next: List<FlowRule>) {
  rules = next
  val arr = JSONArray()
  next.forEach { r -> arr.put(JSONObject().put("id", r.id).put("name", r.name).put("trigger", r.trigger).put("action", r.action).put("enabled", r.enabled)) }
  prefs.edit().putString("rules", arr.toString()).apply()
 }
 fun runAction(action: String) {
  try {
   val intent = when (action) {
    "Open Wi-Fi settings" -> Intent(Settings.ACTION_WIFI_SETTINGS)
    "Open Bluetooth settings" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    "Open display settings" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
    "Open battery settings" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    "Open Do Not Disturb settings" -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    "Open app settings" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
    "Open date and time settings" -> Intent(Settings.ACTION_DATE_SETTINGS)
    else -> Intent(Settings.ACTION_SETTINGS)
   }
   context.startActivity(intent); message = "Opened: $action"
  } catch (_: Exception) { message = "This settings page is not available on this device." }
 }

 val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context) else lightColorScheme()
 MaterialTheme(colorScheme = scheme) {
  Scaffold(
   topBar = { CenterAlignedTopAppBar(title = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("DroidFlow", fontWeight = FontWeight.Bold)
    Text("Your routines, under your control", style = MaterialTheme.typography.labelSmall)
   } }, actions = { IconButton(onClick = { tab = 4 }) { Icon(Icons.Default.Settings, "Settings") } }) },
   bottomBar = { NavigationBar {
    listOf("Home", "Flows", "Routines", "History").forEachIndexed { index, label ->
     val icon = listOf(Icons.Default.Home, Icons.Default.AccountTree, Icons.Default.Bolt, Icons.Default.History)[index]
     NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icon, null) }, label = { Text(label) })
    }
   } },
   floatingActionButton = { if (tab == 1) FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add flow") } }
  ) { padding ->
   LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
    when (tab) {
     0 -> {
      item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(28.dp)) {
       Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Text("Make your phone work your way", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Build simple routines, launch system settings, and keep everyday actions one tap away.")
        FilledTonalButton(onClick = { tab = 1 }) { Icon(Icons.Default.AccountTree, null); Spacer(Modifier.width(8.dp)); Text("Manage flows") }
       }
      } }
      item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
       StatCard("Total flows", rules.size.toString(), Icons.Default.AccountTree, Modifier.weight(1f))
       StatCard("Enabled", rules.count { it.enabled }.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
      } }
      item { Text("Quick actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
      items(listOf("Open Wi-Fi settings", "Open Bluetooth settings", "Open display settings", "Open battery settings")) { action ->
       ActionCard(action, action.removePrefix("Open ").removeSuffix(" settings"), Icons.Default.Bolt) { runAction(action) }
      }
      item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
     }
     1 -> {
      item { Text("My flows", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
      item { Text("User-triggered routines. DroidFlow does not silently control other apps.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
      items(rules, key = { it.id }) { rule ->
       Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
         Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AccountTree, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) { Text(rule.name, fontWeight = FontWeight.SemiBold); Text(rule.action, style = MaterialTheme.typography.bodySmall) }
          Switch(checked = rule.enabled, onCheckedChange = { on -> save(rules.map { if (it.id == rule.id) it.copy(enabled = on) else it }) })
         }
         Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = { runAction(rule.action) }, enabled = rule.enabled, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Run now") }
          OutlinedButton(onClick = { save(rules.filterNot { it.id == rule.id }) }) { Icon(Icons.Default.DeleteOutline, null); Text("Delete") }
         }
        }
       }
      }
     }
     2 -> {
      item { Text("Routine templates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
      item { Text("Add a template as a flow, then run it when you need it.") }
      items(listOf(
       Triple("Study session", "Open Do Not Disturb settings", Icons.Default.School),
       Triple("Connectivity", "Open Wi-Fi settings", Icons.Default.Wifi),
       Triple("Low battery", "Open battery settings", Icons.Default.BatteryAlert),
       Triple("Wind down", "Open display settings", Icons.Default.DarkMode),
       Triple("Bluetooth devices", "Open Bluetooth settings", Icons.Default.Bluetooth),
       Triple("Time setup", "Open date and time settings", Icons.Default.Schedule)
      )) { template ->
       ActionCard(template.first, template.second, template.third) {
        save(rules + FlowRule(System.currentTimeMillis(), template.first, "Manual", template.second, true))
        message = "Added template: " + template.first; tab = 1
       }
      }
     }
     3 -> {
      item { Text("Activity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
      item { Card { Column(Modifier.padding(18.dp)) {
       Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
       Spacer(Modifier.height(8.dp)); Text("Local activity overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
       Text(message); Text("Flow definitions are stored on this device.", style = MaterialTheme.typography.bodySmall)
      } } }
     }
     else -> {
      item { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
      item { Text("DroidFlow uses explicit, user-triggered actions. Android may restrict what an app can change automatically.") }
      items(listOf(
       Triple("App permissions", "Review DroidFlow permissions", Icons.Default.Security),
       Triple("Battery settings", "Manage battery behavior", Icons.Default.BatteryFull),
       Triple("Notifications", "Manage notification settings", Icons.Default.Notifications),
       Triple("System settings", "Open Android settings", Icons.Default.Settings)
      )) { option ->
       ActionCard(option.first, option.second, option.third) {
        val intent = when (option.first) {
         "App permissions" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
         "Battery settings" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
         "Notifications" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
         else -> Intent(Settings.ACTION_SETTINGS)
        }
        try { context.startActivity(intent) } catch (_: Exception) { message = "Setting unavailable." }
       }
      }
      item { Text("Version 1.0 • Offline-first • No account required", style = MaterialTheme.typography.bodySmall) }
     }
    }
   }
  }
  if (showAdd) AlertDialog(
   onDismissRequest = { showAdd = false }, title = { Text("Create a flow") },
   text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Flow name") }, singleLine = true)
    Text("Action", style = MaterialTheme.typography.labelLarge)
    listOf("Open Wi-Fi settings", "Open Bluetooth settings", "Open display settings", "Open battery settings", "Open Do Not Disturb settings", "Open app settings", "Open date and time settings").forEach { action ->
     Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = newAction == action, onClick = { newAction = action }); Text(action, style = MaterialTheme.typography.bodyMedium) }
    }
   } },
   confirmButton = { Button(onClick = {
    if (newName.isNotBlank()) { save(rules + FlowRule(System.currentTimeMillis(), newName.trim(), "Manual", newAction, true)); newName = ""; showAdd = false }
   }) { Text("Create") } },
   dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
  )
 }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
 Card(modifier, shape = RoundedCornerShape(22.dp)) {
  Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
   Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
   Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
   Text(label, style = MaterialTheme.typography.bodyMedium)
  }
 }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
 Card(onClick = onClick, shape = RoundedCornerShape(20.dp)) {
  Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
   Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
    Icon(icon, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
   }
   Spacer(Modifier.width(14.dp))
   Column(Modifier.weight(1f)) {
    Text(title, fontWeight = FontWeight.SemiBold)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
   }
   Icon(Icons.Default.ChevronRight, null)
  }
 }
}
