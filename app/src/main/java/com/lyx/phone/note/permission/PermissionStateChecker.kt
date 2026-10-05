package com.lyx.phone.note.permission

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class AppPermissionState(
    val hasCallLog: Boolean,
    val hasPhoneState: Boolean,
    val hasOverlay: Boolean,
    val callScreeningRoleAvailable: Boolean,
    val hasCallScreeningRole: Boolean
)

object PermissionStateChecker {
    fun check(context: Context): AppPermissionState {
        val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else {
            null
        }
        val roleAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
        return AppPermissionState(
            hasCallLog = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED,
            hasPhoneState = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED,
            hasOverlay = Settings.canDrawOverlays(context),
            callScreeningRoleAvailable = roleAvailable,
            hasCallScreeningRole = roleAvailable &&
                roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        )
    }

    fun overlaySettingsIntent(context: Context) =
        android.content.Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
}
