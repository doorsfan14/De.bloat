package net.habibbatista.debloat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 42;

    private static final Set<String> SAFE_CANDIDATES = new HashSet<>(Arrays.asList(
        "com.samsung.android.app.spage",
        "com.samsung.android.app.tips",
        "com.samsung.android.app.sbrowser",
        "com.samsung.android.app.samsungapps",
        "com.samsung.android.game.gamehome",
        "com.samsung.android.game.gametools",
        "com.samsung.android.app.watchmanagerstub",
        "com.samsung.android.mateagent",
        "com.samsung.android.aremoji",
        "com.samsung.android.arzone",
        "com.samsung.android.ardrawing",
        "com.samsung.android.app.routines",
        "com.samsung.android.app.taskedge",
        "com.samsung.android.app.clipboardedge",
        "com.samsung.android.app.sbrowseredge",
        "com.samsung.android.app.sharelive"
    ));

    private final List<String> selected = new ArrayList<>();
    private LinearLayout packageList;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        updateStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 20);
        root.setBackgroundColor(0xFFF7F7F7);

        TextView title = new TextView(this);
        title.setText("de.bloat");
        title.setTextSize(32);
        title.setTextColor(0xFF202124);

        TextView subtitle = new TextView(this);
        subtitle.setText("One UI cleanup, without touching the system core.");
        subtitle.setTextSize(15);
        subtitle.setTextColor(0xFF5F6368);
        subtitle.setPadding(0, 4, 0, 18);

        status = new TextView(this);
        status.setTextSize(14);
        status.setPadding(0, 0, 0, 14);

        Button refresh = new Button(this);
        refresh.setText("Scan One UI");
        refresh.setOnClickListener(v -> scan());

        packageList = new LinearLayout(this);
        packageList.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(packageList);

        Button debloat = new Button(this);
        debloat.setText("DEBLOAT SELECTED");
        debloat.setOnClickListener(v -> confirmDebloat());

        root.addView(title);
        root.addView(subtitle);
        root.addView(status);
        root.addView(refresh);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(debloat);
        setContentView(root);
    }

    private void updateStatus() {
        if (!Shizuku.pingBinder()) {
            status.setText("Shizuku: not running");
            status.setTextColor(0xFFB00020);
        } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            status.setText("Shizuku: connected, permission needed");
            status.setTextColor(0xFF8A5A00);
        } else {
            status.setText("Shizuku: ready");
            status.setTextColor(0xFF2E7D32);
        }
    }

    private void scan() {
        packageList.removeAllViews();
        selected.clear();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS);
        int found = 0;

        for (ApplicationInfo app : apps) {
            if (!SAFE_CANDIDATES.contains(app.packageName)) continue;

            CheckBox box = new CheckBox(this);
            String label;
            try { label = pm.getApplicationLabel(app).toString(); }
            catch (Exception e) { label = app.packageName; }

            box.setText(label + "\n" + app.packageName);
            box.setTextSize(14);
            box.setPadding(8, 12, 8, 12);
            box.setOnCheckedChangeListener((button, checked) -> {
                if (checked) selected.add(app.packageName);
                else selected.remove(app.packageName);
            });
            packageList.addView(box);
            found++;
        }

        if (found == 0) {
            TextView empty = new TextView(this);
            empty.setText("No supported One UI packages found on this device.");
            empty.setTextSize(15);
            empty.setPadding(8, 24, 8, 24);
            packageList.addView(empty);
        }

        status.setText("Found " + found + " supported packages.");
        status.setTextColor(0xFF202124);
    }

    private void confirmDebloat() {
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one package.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Start Shizuku first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE);
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Disable selected packages?")
            .setMessage("de.bloat will disable the selected apps for the current user. It will not delete their APK files.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Disable", (d, w) -> debloat())
            .show();
    }

    private void debloat() {
        final int total = selected.size();
        new Thread(() -> {
            int success = 0;
            for (String pkg : new ArrayList<>(selected)) {
                try {
                    Process p = Shizuku.newProcess(
                        new String[]{"sh", "-c", "pm disable-user --user 0 " + pkg},
                        null, null
                    );
                    if (p.waitFor() == 0) success++;
                } catch (Exception ignored) {}
            }
            int done = success;
            runOnUiThread(() -> {
                Toast.makeText(this, "Disabled " + done + " of " + total + " packages.", Toast.LENGTH_LONG).show();
                scan();
            });
        }).start();
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
