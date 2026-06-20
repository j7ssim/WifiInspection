package com.wifi.inspection  

import android.Manifest  
import android.content.Context  
import android.content.pm.PackageManager  
import android.net.wifi.ScanResult  
import android.net.wifi.WifiNetworkSpecifier  
import android.net.ConnectivityManager  
import android.net.NetworkRequest  
import android.os.Build  
import android.os.Bundle  
import android.provider.Settings  
import android.content.Intent  
import android.widget.*  
import android.view.Gravity  
import android.view.View  
import android.graphics.Color  
import android.graphics.Typeface  
import android.text.InputType  
import androidx.appcompat.app.AppCompatActivity  
import androidx.core.app.ActivityCompat  
import androidx.core.content.ContextCompat  

class MainActivity : AppCompatActivity() {  

    private val requestCodePermissions = 100  
    private lateinit var listView: ListView  
    private lateinit var statusText: TextView  
    private lateinit var scanButton: Button  
    private lateinit var savedButton: Button  

    private val networks = mutableListOf<ScanResult>()  
    private val savedPasswords = mutableMapOf<String, String>()  

    override fun onCreate(savedInstanceState: Bundle?) {  
        super.onCreate(savedInstanceState)  

        loadSavedPasswords()  
        buildUi()  
        requestNeededPermissions()  
    }  

    private fun buildUi() {  
        val root = LinearLayout(this)  
        root.orientation = LinearLayout.VERTICAL  
        root.setPadding(24, 24, 24, 24)  
        root.setBackgroundColor(Color.rgb(18, 18, 18))  

        val title = TextView(this)  
        title.text = "WIFI INSPECTION"  
        title.textSize = 24f  
        title.setTextColor(Color.WHITE)  
        title.gravity = Gravity.CENTER  
        title.setTypeface(null, Typeface.BOLD)  
        root.addView(title)  

        statusText = TextView(this)  
        statusText.text = "اضغط فحص الشبكات حتى تظهر الشبكات القريبة"  
        statusText.textSize = 16f  
        statusText.setTextColor(Color.LTGRAY)  
        statusText.setPadding(0, 20, 0, 20)  
        statusText.gravity = Gravity.CENTER  
        root.addView(statusText)  

        scanButton = Button(this)  
        scanButton.text = "فحص الشبكات القريبة"  
        root.addView(scanButton)  

        savedButton = Button(this)  
        savedButton.text = "عرض الباسوردات المحفوظة"  
        root.addView(savedButton)  

        listView = ListView(this)  
        root.addView(  
            listView,  
            LinearLayout.LayoutParams(  
                LinearLayout.LayoutParams.MATCH_PARENT,  
                0,  
                1f  
            )  
        )  

        setContentView(root)  

        scanButton.setOnClickListener {  
            scanNetworks()  
        }  

        savedButton.setOnClickListener {  
            showSavedPasswords()  
        }  

        listView.setOnItemClickListener { _, _, position, _ ->  
            val network = networks[position]  
            showNetworkDialog(network)  
        }  
    }  

    private fun requestNeededPermissions() {  
        val permissions = mutableListOf<String>()  

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)  
            != PackageManager.PERMISSION_GRANTED  
        ) {  
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)  
        }  

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)  
                != PackageManager.PERMISSION_GRANTED  
            ) {  
                permissions.add(Manifest.permission.NEAR