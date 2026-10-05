package com.lyx.phone.note.permission

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build

object RoleRequestHelper {
    fun createCallScreeningRequestIntent(activity: Activity): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = activity.getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) return null
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) return null
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }
}
