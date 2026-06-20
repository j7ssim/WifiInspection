package com.wifi.inspection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 100;

    private WifiManager wifiManager;
    private ListView listView;
    private TextView statusText;

    private final List<ScanResult> networks = new ArrayList<>();
    private final HashMap<String, String> savedPasswords = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        loadSavedPasswords();
        buildUi();
        requestNeededPermissions();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(18, 18, 18));

        TextView title = new TextView(this);
        title.setText("WIFI INSPECTION");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        statusText = new TextView(this);
        statusText.setText("اضغط فحص الشبكات حتى تظهر الشبكات القريبة");
        statusText.setTextSize(16);
        statusText.setTextColor(Color.LTGRAY);
        statusText.setPadding(0, 20, 0, 20);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText);

        Button scanButton = new Button(this);
        scanButton.setText("فحص الشبكات القريبة");
        root.addView(scanButton);

        Button savedButton = new Button(this);
        savedButton.setText("عرض الباسوردات المحفوظة");
        root.addView(savedButton);

        listView = new ListView(this);
        root.addView(
                listView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);

        scanButton.setOnClickListener(v -> scanNetworks());
        savedButton.setOnClickListener(v -> showSavedPasswords());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            showNetworkDialog(networks.get(position));
        });
    }

    private void requestNeededPermissions() {
        ArrayList<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissions.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS
            );
        }
    }

    private void scanNetworks() {
        try {
            if (!wifiManager.isWifiEnabled()) {
                Toast.makeText(this, "شغل الواي فاي أولاً", Toast.LENGTH_LONG).show();
                statusText.setText("الواي فاي مطفي");
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();

            networks.clear();
            HashSet<String> seen = new HashSet<>();

            for (ScanResult result : results) {
                if (result.SSID != null && !result.SSID.trim().isEmpty() && !seen.contains(result.SSID)) {
                    networks.add(result);
                    seen.add(result.SSID);
                }
            }

            if (networks.isEmpty()) {
                statusText.setText("ماكو شبكات ظاهرة. تأكد الموقع والواي فاي شغالين.");
                listView.setAdapter(null);
                return;
            }

            statusText.setText("تم العثور على " + networks.size() + " شبكة");

            ArrayList<String> names = new ArrayList<>();
            for (ScanResult network : networks) {
                String saved = savedPasswords.containsKey(network.SSID) ? "محفوظ" : "غير محفوظ";
                names.add(network.SSID + "\nقوة الإشارة: " + network.level + " dBm | " + saved);
            }

            listView.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    names
            ));

        } catch (SecurityException e) {
            Toast.makeText(this, "يحتاج صلاحية الموقع", Toast.LENGTH_LONG).show();
        }
    }

    private void showNetworkDialog(ScanResult network) {
        EditText input = new EditText(this);
        input.setHint("اكتب باسورد الشبكة إذا تعرفه");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String oldPassword = savedPasswords.get(network.SSID);
        if (oldPassword != null) {
            input.setText(oldPassword);
        }

        new AlertDialog.Builder(this)
                .setTitle(network.SSID)
                .setMessage("الحماية: " + network.capabilities + "\nقوة الإشارة: " + network.level + " dBm")
                .setView(input)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String password = input.getText().toString();
                    if (!password.trim().isEmpty()) {
                        savedPasswords.put(network.SSID, password);
                        savePasswords();
                        Toast.makeText(this, "تم حفظ الباسورد", Toast.LENGTH_SHORT).show();
                        scanNetworks();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showSavedPasswords() {
        if (savedPasswords.isEmpty()) {
            Toast.makeText(this, "لا توجد باسوردات محفوظة", Toast.LENGTH_LONG).show();
            return;
        }

        StringBuilder text = new StringBuilder();

        for (String ssid : savedPasswords.keySet()) {
            text.append("الشبكة: ")
                    .append(ssid)
                    .append("\nالباسورد: ")
                    .append(savedPasswords.get(ssid))
                    .append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("الباسوردات المحفوظة")
                .setMessage(text.toString())
                .setPositiveButton("موافق", null)
                .show();
    }

    private void loadSavedPasswords() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("wifi_passwords", Context.MODE_PRIVATE);

        savedPasswords.clear();

        for (String key : prefs.getAll().keySet()) {
            Object value = prefs.getAll().get(key);
            if (value instanceof String) {
                savedPasswords.put(key, (String) value);
            }
        }
    }

    private void savePasswords() {
        android.content.SharedPreferences.Editor editor =
                getSharedPreferences("wifi_passwords", Context.MODE_PRIVATE).edit();

        editor.clear();

        for (String ssid : savedPasswords.keySet()) {
            editor.putString(ssid, savedPasswords.get(ssid));
        }

        editor.apply();
    }
}