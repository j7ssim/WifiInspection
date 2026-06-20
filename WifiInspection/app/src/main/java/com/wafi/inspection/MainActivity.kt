package com.wifi.inspection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requestCodePermissions = 100
    private lateinit var wifiManager: WifiManager
    private lateinit var listView: ListView
    private lateinit var statusText: TextView

    private val networks = mutableListOf<android.net.wifi.ScanResult>()
    private val savedPasswords = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

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

        val scanButton = Button(this)
        scanButton.text = "فحص الشبكات القريبة"
        root.addView(scanButton)

        val savedButton = Button(this)
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
            showNetworkDialog(networks[position])
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                requestCodePermissions
            )
        }
    }

    private fun scanNetworks() {
        try {
            if (!wifiManager.isWifiEnabled) {
                Toast.makeText(this, "شغل الواي فاي أولاً", Toast.LENGTH_LONG).show()
                statusText.text = "الواي فاي مطفي"
                return
            }

            val results = wifiManager.scanResults
            networks.clear()
            networks.addAll(results.filter { it.SSID.isNotBlank() }.distinctBy { it.SSID })

            if (networks.isEmpty()) {
                statusText.text = "ماكو شبكات ظاهرة. تأكد الموقع والواي فاي شغالين."
                listView.adapter = null
                return
            }

            statusText.text = "تم العثور على ${networks.size} شبكة"

            val names = networks.map {
                val saved = if (savedPasswords.containsKey(it.SSID)) "محفوظ" else "غير محفوظ"
                "${it.SSID}\nقوة الإشارة: ${it.level} dBm | $saved"
            }

            listView.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                names
            )
        } catch (e: SecurityException) {
            Toast.makeText(this, "يحتاج صلاحية الموقع", Toast.LENGTH_LONG).show()
        }
    }

    private fun showNetworkDialog(network: android.net.wifi.ScanResult) {
        val input = EditText(this)
        input.hint = "اكتب باسورد الشبكة إذا تعرفه"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        val oldPassword = savedPasswords[network.SSID]
        if (oldPassword != null) {
            input.setText(oldPassword)
        }

        AlertDialog.Builder(this)
            .setTitle(network.SSID)
            .setMessage("الحماية: ${network.capabilities}\nقوة الإشارة: ${network.level} dBm")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val password = input.text.toString()
                if (password.isNotBlank()) {
                    savedPasswords[network.SSID] = password
                    savePasswords()
                    Toast.makeText(this, "تم حفظ الباسورد", Toast.LENGTH_SHORT).show()
                    scanNetworks()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showSavedPasswords() {
        if (savedPasswords.isEmpty()) {
            Toast.makeText(this, "لا توجد باسوردات محفوظة", Toast.LENGTH_LONG).show()
            return
        }

        val text = savedPasswords.entries.joinToString("\n\n") {
            "الشبكة: ${it.key}\nالباسورد: ${it.value}"
        }

        AlertDialog.Builder(this)
            .setTitle("الباسوردات المحفوظة")
            .setMessage(text)
            .setPositiveButton("موافق", null)
            .show()
    }

    private fun loadSavedPasswords() {
        val prefs = getSharedPreferences("wifi_passwords", Context.MODE_PRIVATE)
        savedPasswords.clear()

        prefs.all.forEach { item ->
            val value = item.value
            if (value is String) {
                savedPasswords[item.key] = value
            }
        }
    }

    private fun savePasswords() {
        val prefs = getSharedPreferences("wifi_passwords", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear()

        savedPasswords.forEach { item ->
            editor.putString(item.key, item.value)
        }

        editor.apply()
    }
}
