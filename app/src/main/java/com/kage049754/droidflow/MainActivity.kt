package com.kage049754.droidflow

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.text.DateFormat
import java.util.Date

private val actions = listOf("Open Wi-Fi settings","Open Bluetooth settings","Open display settings","Open battery settings","Open Do Not Disturb settings","Open app settings","Open date and time settings","Open Android settings")
data class FlowRule(val id: Long, val name: String, val trigger: String, val action: String, val enabled: Boolean)
data class FlowLog(val id: Long, val title: String, val detail: String, val time: Long)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { DroidFlowApp() } }
}
private val seedRules = listOf(
 FlowRule(1,"Study focus","Manual","Open Do Not Disturb settings",true),
 FlowRule(2,"Battery saver","Manual","Open battery settings",false),
 FlowRule(3,"Night routine","Manual","Open display settings",false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidFlowApp() {
 val context = LocalContext.current
 val prefs = remember { context.getSharedPreferences("droidflow",0) }
 fun readRules(): List<FlowRule> = try {
  val raw = prefs.getString("rules", null)
  if (raw.isNullOrBlank()) seedRules else {
   val a = JSONArray(raw)
   (0 until a.length()).map { a.getJSONObject(it).let { o -> FlowRule(o.getLong("id"),o.getString("name"),o.optString("trigger","Manual"),o.getString("action"),o.optBoolean("enabled",true)) } }
  }
 } catch (_:Exception) { seedRules }
 fun readLogs(): List<FlowLog> = try {
  val a=JSONArray(prefs.getString("history","[]"))
  (0 until a.length()).map { a.getJSONObject(it).let { o -> FlowLog(o.getLong("id"),o.getString("title"),o.getString("detail"),o.getLong("time")) } }.sortedByDescending { it.time }
 } catch (_:Exception) { emptyList() }
 var rules by remember { mutableStateOf(readRules()) }
 var logs by remember { mutableStateOf(readLogs()) }
 var tab by remember { mutableIntStateOf(0) }
 var editor by remember { mutableStateOf(false) }
 var editId by remember { mutableStateOf<Long?>(null) }
 var name by remember { mutableStateOf("") }
 var action by remember { mutableStateOf(actions.first()) }
 var search by remember { mutableStateOf("") }
 var status by remember { mutableStateOf("Flows run only when you choose them.") }
 var importedRules by remember { mutableStateOf<List<FlowRule>?>(null) }
 var importError by remember { mutableStateOf<String?>(null) }
 val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if (uri != null) {
   try {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Could not read the selected file.")
    require(text.length <= 1_000_000) { "Backup file is too large." }
    val array = JSONArray(text)
    require(array.length() in 1..200) { "Backup must contain between 1 and 200 flows." }
    val parsed = (0 until array.length()).map { i ->
     val obj = array.getJSONObject(i)
     val flowName = obj.optString("name").trim()
     val flowAction = obj.optString("action")
     require(flowName.isNotBlank() && flowName.length <= 60) { "A flow has an invalid name." }
     require(flowAction in actions) { "Backup contains an unsupported action." }
     FlowRule(System.currentTimeMillis() + i, flowName, "Manual", flowAction, obj.optBoolean("enabled", true))
    }
    importedRules = parsed
    importError = null
   } catch (e: Exception) { importError = e.message ?: "Invalid backup file." }
  }
 }

 fun saveRules(next:List<FlowRule>) {
  rules=next; val a=JSONArray()
  next.forEach { a.put(JSONObject().put("id",it.id).put("name",it.name).put("trigger",it.trigger).put("action",it.action).put("enabled",it.enabled)) }
  prefs.edit().putString("rules",a.toString()).apply()
 }
 fun log(title:String,detail:String) {
  val next=(listOf(FlowLog(System.currentTimeMillis(),title,detail,System.currentTimeMillis()))+logs).take(100)
  logs=next; val a=JSONArray()
  next.forEach { a.put(JSONObject().put("id",it.id).put("title",it.title).put("detail",it.detail).put("time",it.time)) }
  prefs.edit().putString("history",a.toString()).apply()
 }
 fun runAction(value:String,title:String=value) {
  try {
   val intent=when(value) {
    "Open Wi-Fi settings" -> Intent(Settings.ACTION_WIFI_SETTINGS)
    "Open Bluetooth settings" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    "Open display settings" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
    "Open battery settings" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    "Open Do Not Disturb settings" -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    "Open app settings" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+context.packageName))
    "Open date and time settings" -> Intent(Settings.ACTION_DATE_SETTINGS)
    else -> Intent(Settings.ACTION_SETTINGS)
   }
   context.startActivity(intent); status="Opened: $value"; log(title,"Launched settings")
  } catch (_:Exception) { status="Settings page unavailable on this device."; log(title,"Unavailable on this device") }
 }
 fun edit(rule:FlowRule?=null) { editId=rule?.id; name=rule?.name ?: ""; action=rule?.action ?: actions.first(); editor=true }

 val colors=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S) dynamicLightColorScheme(context) else lightColorScheme()
 MaterialTheme(colorScheme=colors) {
  Scaffold(
   topBar={ CenterAlignedTopAppBar(title={ Column(horizontalAlignment=Alignment.CenterHorizontally) {
    Text("DroidFlow",fontWeight=FontWeight.Bold); Text("Build routines. Stay in control.",style=MaterialTheme.typography.labelSmall)
   } },actions={ IconButton(onClick={tab=4}) { Icon(Icons.Default.Settings,"Settings") } }) },
   bottomBar={ NavigationBar {
    listOf("Home","Flows","Templates","History").forEachIndexed { i,label ->
     val icons=listOf(Icons.Default.Home,Icons.Default.AccountTree,Icons.Default.Widgets,Icons.Default.History)
     NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],null)},label={Text(label)})
    }
   } },
   floatingActionButton={ if(tab==1) FloatingActionButton(onClick={edit()}) { Icon(Icons.Default.Add,"Create flow") } }
  ) { padding ->
   LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=12.dp,bottom=28.dp)) {
    when(tab) {
     0 -> {
      item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(28.dp)) {
       Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.AutoAwesome,null,modifier=Modifier.size(36.dp))
        Text("Your phone. Your routines.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Keep useful Android actions organized and ready to launch. Every flow runs only after you tap Run.")
        FilledTonalButton(onClick={tab=1}) { Icon(Icons.Default.AccountTree,null); Spacer(Modifier.width(8.dp)); Text("Open my flows") }
       }
      } }
      item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
       StatCard("Total flows",rules.size.toString(),Icons.Default.AccountTree,Modifier.weight(1f))
       StatCard("Enabled",rules.count{it.enabled}.toString(),Icons.Default.CheckCircle,Modifier.weight(1f))
      } }
      item { Text("Quick actions",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
      items(actions.take(4)) { a -> ActionCard(a,a.removePrefix("Open ").removeSuffix(" settings"),Icons.Default.Bolt) { runAction(a) } }
      item { Text(status,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
     }
     1 -> {
      item { Text("My flows",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
      item { OutlinedTextField(value=search,onValueChange={search=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search flows")},leadingIcon={Icon(Icons.Default.Search,null)}) }
      item { Text("Manual flows • "+rules.size+" saved",color=MaterialTheme.colorScheme.onSurfaceVariant) }
      val filtered=rules.filter{it.name.contains(search,true)||it.action.contains(search,true)}
      if(filtered.isEmpty()) item { EmptyCard("No matching flows","Create a flow or try another search.") }
      items(filtered,key={it.id}) { r ->
       Card(shape=RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
         Row(verticalAlignment=Alignment.CenterVertically) {
          Icon(Icons.Default.AccountTree,null,tint=MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) { Text(r.name,fontWeight=FontWeight.SemiBold); Text(r.action,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
          Switch(checked=r.enabled,onCheckedChange={v->saveRules(rules.map{if(it.id==r.id)it.copy(enabled=v) else it})})
         }
         Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
          Button(onClick={runAction(r.action,r.name)},enabled=r.enabled,modifier=Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow,null); Text("Run") }
          IconButton(onClick={edit(r)}) { Icon(Icons.Default.Edit,"Edit") }
          IconButton(onClick={saveRules(rules+r.copy(id=System.currentTimeMillis(),name=r.name+" copy"))}) { Icon(Icons.Default.ContentCopy,"Duplicate") }
          IconButton(onClick={saveRules(rules.filterNot{it.id==r.id});log(r.name,"Deleted flow")}) { Icon(Icons.Default.DeleteOutline,"Delete") }
         }
        }
       }
      }
     }
     2 -> {
      item { Text("Routine templates",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
      item { Text("Add a ready-made shortcut to your flows. Customize it anytime.") }
      val templates=listOf(Triple("Study focus","Open Do Not Disturb settings",Icons.Default.School),Triple("Wi-Fi access","Open Wi-Fi settings",Icons.Default.Wifi),Triple("Battery check","Open battery settings",Icons.Default.BatteryAlert),Triple("Wind down","Open display settings",Icons.Default.DarkMode),Triple("Bluetooth setup","Open Bluetooth settings",Icons.Default.Bluetooth),Triple("Time settings","Open date and time settings",Icons.Default.Schedule))
      items(templates) { t -> ActionCard(t.first,t.second,t.third) {
       saveRules(rules+FlowRule(System.currentTimeMillis(),t.first,"Manual",t.second,true)); log(t.first,"Added template"); status="Added "+t.first; tab=1
      } }
     }
     3 -> {
      item { Text("Activity history",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
      item { Text("Latest 100 events are saved on this device.") }
      if(logs.isEmpty()) item { EmptyCard("No activity yet","Run a flow or add a template to see activity here.") }
      items(logs,key={it.id}) { l -> Card(shape=RoundedCornerShape(18.dp)) {
       Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(Icons.Default.History,null,tint=MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
         Text(l.title,fontWeight=FontWeight.SemiBold); Text(l.detail,style=MaterialTheme.typography.bodySmall)
         Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(l.time)),style=MaterialTheme.typography.labelSmall)
        }
       }
      } }
      if(logs.isNotEmpty()) item { OutlinedButton(onClick={logs=emptyList();prefs.edit().putString("history","[]").apply()},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.DeleteSweep,null); Text("Clear history") } }
     }
     else -> {
      item { Text("Settings & backup",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
      item { Text("Your flows and history stay in local app storage. No account or cloud sync is used.") }
      item { ActionCard("Import flows","Restore routines from a DroidFlow JSON backup",Icons.Default.FileDownload) { importLauncher.launch(arrayOf("application/json","text/*","application/octet-stream")) } }
      item { ActionCard("Export flows","Share a JSON backup of your routines",Icons.Default.FileUpload) {
       val data=JSONArray()
       rules.forEach { data.put(JSONObject().put("name",it.name).put("trigger",it.trigger).put("action",it.action).put("enabled",it.enabled)) }
       val send=Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_SUBJECT,"DroidFlow backup").putExtra(Intent.EXTRA_TEXT,data.toString(2))
       context.startActivity(Intent.createChooser(send,"Export DroidFlow flows")); log("Backup","Exported flows")
      } }
      item { ActionCard("App permissions","Review DroidFlow permissions",Icons.Default.Security) { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+context.packageName))) } }
      item { ActionCard("Battery settings","Manage battery behavior",Icons.Default.BatteryFull) { runAction("Open battery settings") } }
      item { ActionCard("Notifications","Manage app notification settings",Icons.Default.Notifications) {
       try { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)) } catch(_:Exception) { runAction("Open Android settings") }
      } }
      item { ActionCard("System settings","Open Android settings",Icons.Default.Settings) { runAction("Open Android settings") } }
      item { Text("DroidFlow • Offline-first • No account • No ads",style=MaterialTheme.typography.bodySmall) }
     }
    }
   }
  }
  if (importedRules != null) AlertDialog(
   onDismissRequest = { importedRules = null },
   title = { Text("Restore backup?") },
   text = { Text("Replace your current ${rules.size} flows with ${importedRules!!.size} validated flows from this backup? Your activity history will remain.") },
   confirmButton = { Button(onClick = {
    saveRules(importedRules!!); log("Backup", "Restored ${importedRules!!.size} flows"); importedRules = null; status = "Backup restored."
   }) { Text("Restore") } },
   dismissButton = { TextButton(onClick = { importedRules = null }) { Text("Cancel") } }
  )
  if (importError != null) AlertDialog(
   onDismissRequest = { importError = null }, title = { Text("Import failed") },
   text = { Text(importError!!) }, confirmButton = { TextButton(onClick = { importError = null }) { Text("OK") } }
  )
  if(editor) AlertDialog(onDismissRequest={editor=false},title={Text(if(editId==null)"Create a flow" else "Edit flow")},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(value=name,onValueChange={name=it},label={Text("Flow name")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Text("Action",style=MaterialTheme.typography.labelLarge)
    actions.forEach { a -> Row(verticalAlignment=Alignment.CenterVertically) { RadioButton(selected=action==a,onClick={action=a}); Text(a,style=MaterialTheme.typography.bodyMedium) } }
   } },
   confirmButton={Button(enabled=name.isNotBlank(),onClick={
    val id=editId
    val next=if(id==null) rules+FlowRule(System.currentTimeMillis(),name.trim(),"Manual",action,true) else rules.map{if(it.id==id)it.copy(name=name.trim(),action=action)else it}
    saveRules(next); log(name.trim(),if(id==null)"Created flow" else "Edited flow"); editor=false
   }){Text("Save")} },
   dismissButton={TextButton(onClick={editor=false}){Text("Cancel")}}
  )
 }
}

@Composable private fun StatCard(label:String,value:String,icon:ImageVector,modifier:Modifier=Modifier) {
 Card(modifier,shape=RoundedCornerShape(22.dp)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Icon(icon,null,tint=MaterialTheme.colorScheme.primary); Text(value,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); Text(label)
 } }
}
@Composable private fun ActionCard(title:String,subtitle:String,icon:ImageVector,onClick:()->Unit) {
 Card(onClick=onClick,shape=RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
  Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.secondaryContainer) { Icon(icon,null,Modifier.padding(12.dp),tint=MaterialTheme.colorScheme.onSecondaryContainer) }
  Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title,fontWeight=FontWeight.SemiBold); Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight,null)
 } }
}
@Composable private fun EmptyCard(title:String,subtitle:String) {
 Card(shape=RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
  Icon(Icons.Default.Inbox,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(32.dp)); Spacer(Modifier.height(8.dp)); Text(title,fontWeight=FontWeight.SemiBold); Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 } }
}
