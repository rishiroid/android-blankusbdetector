package com.blankusbdetector

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.storage.StorageManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object{
        private val TAG=MainActivity::class.java.canonicalName
    }

    private val androidAutoCreatedDir= arrayListOf("Android","LOST.DIR","Music","Podcasts","Ringtones","Alarams","Notifications",
    "Pictures","Movies","Download","DCIM","Documents","Audiobooks","Recordings")
    lateinit var textView: TextView
    private val ACTION_USB_PERMISSION = "com.blankusbdetector.USB_PERMISSION"
    private var usbManager: UsbManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager?
        val filter = IntentFilter()
        textView = findViewById(R.id.text)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter)

    }

    override fun onStart() {
        super.onStart()
        callStorageManager(applicationContext)
    }

    private val usbReceiver:BroadcastReceiver=object:BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                Toast.makeText(context, "USB Mounted", Toast.LENGTH_LONG).show()
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (device != null) {
                    val permissionIntent =
                        PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
                    usbManager?.requestPermission(device, permissionIntent)

                    val handler = Handler()
                    val runnable =
                        Runnable {
                            callStorageManager(context)
                        }
                    handler.postDelayed(runnable, 2000)

                } }else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                    Toast.makeText(context, "USB UnMounted", Toast.LENGTH_LONG).show()
                    textView.text = ""

                }

            }

        }



    private fun callStorageManager(context: Context?) {
        textView.text=""
        val storageManager = context?.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        for (storageVolume in storageManager.storageVolumes) {
            if (storageVolume.directory?.path != null && storageVolume.isRemovable) {
                textView.append("Description:" + storageVolume.getDescription(context) + "\n")
                textView.append("Path:" + storageVolume.directory?.path + "\n")
//                val directory = storageVolume.directory?.path?.let { File(it) }
                val directory= storageVolume.directory?.path?.let { File(it) }
                directory?.setReadOnly()
                val files = directory?.listFiles()

                val usbFileList =
                    files?.filter { file -> (androidAutoCreatedDir.contains(file.name) && file.isDirectory) }
                        ?.toList()
                textView.append("usbFileList:$files\n")
                Log.d(
                    TAG,
                    "usbFileList:Size:isEmpty " + usbFileList + ":" + usbFileList?.size + ":" + usbFileList?.isEmpty()
                )

                    if (usbFileList?.isEmpty() == true) {
                        textView.append("IsBlankUsb:Yes")
                    } else {
                        textView.append("IsBlankUsb:No")
                    }


            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }
}