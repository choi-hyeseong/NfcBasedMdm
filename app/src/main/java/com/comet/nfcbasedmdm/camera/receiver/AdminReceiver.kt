package com.comet.nfcbasedmdm.camera.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

// admin 활성화에 필요한 BroadcastReceiver
// Android 10이상 부터 카메라 활성화여부 조절이 어려워져 사실상 명목상 존재..
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Device Admin Receiver Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Device Admin Receiver Disabled", Toast.LENGTH_SHORT).show()
    }



}