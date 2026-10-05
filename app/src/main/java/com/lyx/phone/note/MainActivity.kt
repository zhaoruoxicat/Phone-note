package com.lyx.phone.note

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lyx.phone.note.backup.BackupJson
import com.lyx.phone.note.permission.PermissionStateChecker
import com.lyx.phone.note.permission.RoleRequestHelper
import com.lyx.phone.note.ui.calllog.CallLogScreen
import com.lyx.phone.note.ui.calllog.CallLogViewModel
import com.lyx.phone.note.ui.categories.CategoryManageScreen
import com.lyx.phone.note.ui.categories.CategoryManageViewModel
import com.lyx.phone.note.ui.noteedit.NoteEditScreen
import com.lyx.phone.note.ui.noteedit.NoteEditViewModel
import com.lyx.phone.note.ui.notes.NotesScreen
import com.lyx.phone.note.ui.notes.NotesViewModel
import com.lyx.phone.note.ui.settings.SettingsScreen
import com.lyx.phone.note.ui.theme.LocalCallNoteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val app: LocalCallNoteApp
        get() = application as LocalCallNoteApp

    private var pendingBackupText: String? = null

    private val createBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        val text = pendingBackupText ?: return@registerForActivityResult
        runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        }.onSuccess {
            Toast.makeText(this, "备份已导出", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "导出失败：${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val importBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        importBackup(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalCallNoteTheme {
                AppRoot(
                    activity = this,
                    app = app,
                    startNormalizedNumber = intent?.getStringExtra("normalizedNumber"),
                    onExportBackup = ::exportBackup,
                    onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "text/*")) }
                )
            }
        }
    }

    private fun exportBackup() {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                BackupJson.export(app.repository.getAllForBackup())
            }
            pendingBackupText = text
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            createBackupLauncher.launch("local_call_notes_backup_$stamp.json")
        }
    }

    private fun importBackup(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val notes = BackupJson.parse(text)
                    notes.forEach { app.repository.upsert(it.copy(id = 0, updatedAt = System.currentTimeMillis())) }
                    notes.size
                }
            }
            Toast.makeText(
                this@MainActivity,
                result.fold({ "已导入 $it 条备注" }, { "导入失败：${it.message}" }),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private enum class MainTab(val route: String, val label: String) {
    CALLS("calls", "通话"),
    NOTES("notes", "备注"),
    SETTINGS("settings", "设置")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    activity: Activity,
    app: LocalCallNoteApp,
    startNormalizedNumber: String?,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val navController = rememberNavController()
    var permissionState by remember { mutableStateOf(PermissionStateChecker.check(activity)) }
    val callLogPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionState = PermissionStateChecker.check(activity)
    }
    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionState = PermissionStateChecker.check(activity)
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionState = PermissionStateChecker.check(activity)
    }
    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionState = PermissionStateChecker.check(activity)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            app.categoryRepository.ensureDefaults()
        }
    }
    LaunchedEffect(startNormalizedNumber) {
        startNormalizedNumber?.let { navController.navigateToEdit(raw = it, normalized = it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("本地来电备注") }) },
        bottomBar = { BottomNav(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.CALLS.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(MainTab.CALLS.route) {
                val vm: CallLogViewModel = viewModel(factory = CallLogViewModel.Factory(app.callLogReader))
                CallLogScreen(
                    viewModel = vm,
                    onEditNumber = { raw, normalized -> navController.navigateToEdit(raw, normalized) },
                    onRequestPermission = { callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) }
                )
            }
            composable(MainTab.NOTES.route) {
                val vm: NotesViewModel = viewModel(factory = NotesViewModel.Factory(app.repository))
                NotesScreen(vm) { normalized -> navController.navigateToEdit(normalized, normalized) }
            }
            composable(MainTab.SETTINGS.route) {
                SettingsScreen(
                    permissionState = permissionState,
                    onRequestCallLog = { callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                    onRequestPhoneState = { phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                    onRequestRole = {
                        RoleRequestHelper.createCallScreeningRequestIntent(activity)?.let(roleLauncher::launch)
                            ?: Toast.makeText(activity, "当前系统不可请求来电识别角色", Toast.LENGTH_SHORT).show()
                    },
                    onRequestOverlay = { overlayLauncher.launch(PermissionStateChecker.overlaySettingsIntent(activity)) },
                    onManageCategories = { navController.navigate("categories") },
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup
                )
            }
            composable("categories") {
                val vm: CategoryManageViewModel = viewModel(
                    factory = CategoryManageViewModel.Factory(app.categoryRepository)
                )
                CategoryManageScreen(vm)
            }
            composable(
                route = "edit?raw={raw}&normalized={normalized}",
                arguments = listOf(
                    navArgument("raw") { type = NavType.StringType; defaultValue = "" },
                    navArgument("normalized") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { entry ->
                val raw = entry.arguments?.getString("raw").orEmpty()
                val normalized = entry.arguments?.getString("normalized")
                val vm: NoteEditViewModel = viewModel(
                    factory = NoteEditViewModel.Factory(app.repository, app.categoryRepository, raw, normalized)
                )
                NoteEditScreen(vm) {
                    navController.popBackStack()
                }
            }
        }
    }
}

@Composable
private fun BottomNav(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    NavigationBar {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(MainTab.CALLS.route)
                        launchSingleTop = true
                    }
                },
                label = { Text(tab.label) },
                icon = { BottomNavIcon(tab) }
            )
        }
    }
}

@Composable
private fun BottomNavIcon(tab: MainTab) {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val iconScale = size.minDimension / 24f
        val stroke = Stroke(width = 2.4f, cap = StrokeCap.Round)
        scale(iconScale, pivot = Offset.Zero) {
            when (tab) {
                MainTab.CALLS -> {
                    drawArc(color, 135f, 270f, false, topLeft = Offset(5f, 5f), size = Size(14f, 14f), style = stroke)
                    drawLine(color, Offset(7f, 18f), Offset(11f, 22f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(17f, 2f), Offset(21f, 6f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                MainTab.NOTES -> {
                    drawRoundRect(color, topLeft = Offset(5f, 3f), size = Size(14f, 18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f), style = stroke)
                    drawLine(color, Offset(8f, 8f), Offset(16f, 8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(8f, 12f), Offset(15f, 12f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(8f, 16f), Offset(13f, 16f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                MainTab.SETTINGS -> {
                    translate(left = 12f, top = 12f) {
                        drawCircle(color, radius = 3.2f, style = stroke)
                        repeat(8) { index ->
                            val angle = Math.toRadians((index * 45).toDouble())
                            val start = Offset((6f * kotlin.math.cos(angle)).toFloat(), (6f * kotlin.math.sin(angle)).toFloat())
                            val end = Offset((9f * kotlin.math.cos(angle)).toFloat(), (9f * kotlin.math.sin(angle)).toFloat())
                            drawLine(color, start, end, strokeWidth = stroke.width, cap = StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateToEdit(raw: String, normalized: String?) {
    val rawEncoded = Uri.encode(raw)
    val normalizedEncoded = Uri.encode(normalized.orEmpty())
    navigate("edit?raw=$rawEncoded&normalized=$normalizedEncoded")
}
