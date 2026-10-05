package com.lyx.phone.note.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyx.phone.note.permission.AppPermissionState

@Composable
fun SettingsScreen(
    permissionState: AppPermissionState,
    onRequestCallLog: () -> Unit,
    onRequestPhoneState: () -> Unit,
    onRequestRole: () -> Unit,
    onRequestOverlay: () -> Unit,
    onManageCategories: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("权限与本地数据", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        PermissionItem(
            title = "通话记录权限",
            desc = "用于读取最近通话，方便给陌生号码添加本地备注。",
            enabled = permissionState.hasCallLog,
            button = "去授权",
            onClick = onRequestCallLog
        )
        PermissionItem(
            title = "电话状态权限",
            desc = "用于在电话接听或挂断时自动关闭悬浮提示。",
            enabled = permissionState.hasPhoneState,
            button = "去授权",
            onClick = onRequestPhoneState
        )
        PermissionItem(
            title = "来电识别服务",
            desc = if (permissionState.callScreeningRoleAvailable) {
                "用于来电时获取号码，显示本地备注或按备注设置拒接。"
            } else {
                "当前系统不支持 Call Screening 角色。"
            },
            enabled = permissionState.hasCallScreeningRole,
            button = "去开启",
            onClick = onRequestRole
        )
        PermissionItem(
            title = "悬浮窗权限",
            desc = "用于在来电界面上方显示轻量备注提示。",
            enabled = permissionState.hasOverlay,
            button = "去设置",
            onClick = onRequestOverlay
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("分类管理", fontWeight = FontWeight.SemiBold)
                Text("添加、编辑或删除分类，备注编辑页会同步显示。")
                Button(onClick = onManageCategories, modifier = Modifier.fillMaxWidth()) {
                    Text("管理分类")
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("备份与恢复", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onExportBackup, modifier = Modifier.weight(1f)) { Text("导出 JSON") }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) { Text("导入 JSON") }
                }
            }
        }
        Text(
            "隐私说明：所有号码备注仅保存在本机；不上传通话记录，不上传电话号码，不读取短信，不写入系统通讯录，不进行云端号码识别。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("国产系统兼容建议", fontWeight = FontWeight.SemiBold)
                Text("MIUI/澎湃 OS、ColorOS、OriginOS、HarmonyOS/MagicOS 可能需要额外允许后台运行、自启动、后台弹出界面。若悬浮窗不显示或拦截不生效，请确认来电识别服务仍为本应用。")
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    desc: String,
    enabled: Boolean,
    button: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (enabled) "状态：已开启" else "状态：未开启",
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            OutlinedButton(onClick = onClick, enabled = !enabled) {
                Text(button)
            }
        }
    }
}
