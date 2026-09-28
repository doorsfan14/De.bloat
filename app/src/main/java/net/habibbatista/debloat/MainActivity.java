package net.habibbatista.debloat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
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
import java.util.Locale;
import java.util.Set;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int SHIZUKU_REQUEST_CODE = 42;

    private static final Set<String> KNOWN_BLOAT = new HashSet<>(Arrays.asList(
        "com.samsung.android.app.spage", "com.samsung.android.app.tips",
        "com.samsung.android.app.samsungapps", "com.samsung.android.game.gamehome",
        "com.samsung.android.game.gametools", "com.samsung.android.app.watchmanagerstub",
        "com.samsung.android.mateagent", "com.samsung.android.aremoji",
        "com.samsung.android.arzone", "com.samsung.android.ardrawing",
        "com.samsung.android.app.routines", "com.samsung.android.app.taskedge",
        "com.samsung.android.app.clipboardedge", "com.samsung.android.app.sbrowseredge",
        "com.samsung.android.app.sharelive"
    ));

    private static final String[] BLOAT_KEYWORDS = {
        "promo", "promotion", "tips", "partner", "stub", "demo", "trial",
        "shopping", "booking", "facebook", "linkedin", "microsoft", "netflix",
        "spotify", "amazon", "ebay", "games", "gamehome", "gametools",
        "arzone", "aremoji", "watchmanager", "sharelive", "clipboardedge"
    };

    private static final String[] CORE_PREFIXES = {
        "android", "com.android.", "com.google.android.gms",
        "com.google.android.gsf", "com.google.android.permissioncontroller",
        "com.google.android.packageinstaller", "com.samsung.android.providers.",
        "com.samsung.android.systemui", "com.samsung.android.app.telephonyui",
        "com.samsung.android.incallui", "com.samsung.android.dialer",
        "com.samsung.android.settings", "com.samsung.android.launcher",
        "com.sec.android.app.launcher", "com.sec.android.app.servicemodeapp"
    };

    private final List<String> selected = new ArrayList<>();
    private LinearLayout packageList;
    private TextView status, count;
    private Button debloatButton;
    private final Shizuku.OnRequestPermissionResultListener permissionListener =
        (requestCode, grantResult) -> {
            if (requestCode == SHIZUKU_REQUEST_CODE) runOnUiThread(this::updateShizukuStatus);
        };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        buildUi();
        updateShizukuStatus();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color, int style) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, style);
        return v;
    }

    private LinearLayout card() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(18), dp(16), dp(18), dp(16));
        v.setBackgroundColor(Color.WHITE);
        return v;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(16));
        root.setBackgroundColor(Color.rgb(246, 247, 249));

        TextView title = text("de.bloat", 34, Color.rgb(25,25,28), Typeface.BOLD);
        TextView subtitle = text(
            "Find removable bloatware from your device — OEM, carrier and partner apps.",
            15, Color.rgb(92,94,100), Typeface.NORMAL
        );
        subtitle.setPadding(0, dp(3), 0, dp(14));

        LinearLayout shizukuCard = card();
        shizukuCard.addView(text("Shizuku", 17, Color.rgb(25,25,28), Typeface.BOLD));
        status = text("Checking…", 14, Color.rgb(92,94,100), Typeface.NORMAL);
        status.setPadding(0, dp(4), 0, dp(10));
        Button permission = new Button(this);
        permission.setText("GRANT SHIZUKU ACCESS");
        permission.setAllCaps(false);
        permission.setOnClickListener(v -> requestShizukuPermission());
        shizukuCard.addView(status);
        shizukuCard.addView(permission);

        LinearLayout scanCard = card();
        count = text("No scan yet", 16, Color.rgb(25,25,28), Typeface.BOLD);
        TextView hint = text(
            "Candidates are filtered conservatively. Nothing changes until you confirm.",
            13, Color.rgb(92,94,100), Typeface.NORMAL
        );
        hint.setPadding(0, dp(5), 0, dp(12));
        Button scan = new Button(this);
        scan.setText("SCAN FOR BLOATWARE");
        scan.setAllCaps(false);
        scan.setOnClickListener(v -> scan());
        scanCard.addView(count);
        scanCard.addView(hint);
        scanCard.addView(scan);

        packageList = new LinearLayout(this);
        packageList.setOrientation(LinearLayout.VERTICAL);
        packageList.setPadding(0, dp(10), 0, dp(10));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(packageList);

        debloatButton = new Button(this);
        debloatButton.setText("DEBLOAT SELECTED");
        debloatButton.setAllCaps(false);
        debloatButton.setTextSize(16);
        debloatButton.setEnabled(false);
        debloatButton.setOnClickListener(v -> confirmDebloat());

        root.addView(title);
        root.addView(subtitle);
        root.addView(shizukuCard);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(10);
        root.addView(scanCard, sp);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(debloatButton);
        setContentView(root);
    }

    private void updateShizukuStatus() {
        boolean connected = Shizuku.pingBinder();
        boolean granted = connected &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;

        if (!connected) {
            status.setText("Not running. Start Shizuku, then return here.");
            status.setTextColor(Color.rgb(180,45,45));
        } else if (!granted) {
            status.setText("Running, but de.bloat needs permission.");
            status.setTextColor(Color.rgb(160,105,20));
        } else {
            status.setText("Connected and ready.");
            status.setTextColor(Color.rgb(35,125,70));
        }
    }

    private void requestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Start Shizuku first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku access is already granted.", Toast.LENGTH_SHORT).show();
            return;
        }
        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
    }

    private boolean isCore(ApplicationInfo app) {
        String pkg = app.packageName.toLowerCase(Locale.ROOT);
        for (String prefix : CORE_PREFIXES) {
            if (pkg.equals(prefix) || pkg.startsWith(prefix)) return true;
        }
        return false;
    }

    private boolean looksLikeBloat(ApplicationInfo app, String label) {
        String pkg = app.packageName.toLowerCase(Locale.ROOT);
        String name = label.toLowerCase(Locale.ROOT);
        if (isCore(app)) return false;
        if (KNOWN_BLOAT.contains(pkg)) return true;

        boolean system = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ||
                         (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        if (!system) return false;

        for (String keyword : BLOAT_KEYWORDS) {
            if (pkg.contains(keyword) || name.contains(keyword)) return true;
        }

        return pkg.startsWith("com.samsung.") ||
               pkg.startsWith("com.sec.") ||
               pkg.startsWith("com.microsoft.") ||
               pkg.startsWith("com.facebook.") ||
               pkg.startsWith("com.spotify.") ||
               pkg.startsWith("com.netflix.");
    }

    private void scan() {
        packageList.removeAllViews();
        selected.clear();
        debloatButton.setEnabled(false);
        debloatButton.setText("DEBLOAT SELECTED");

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps =
            pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS);
        int found = 0;

        for (ApplicationInfo app : apps) {
            String label;
            try { label = pm.getApplicationLabel(app).toString(); }
            catch (Exception e) { label = app.packageName; }
            if (!looksLikeBloat(app, label)) continue;

            CheckBox box = new CheckBox(this);
            box.setText(label + "\n" + app.packageName);
            box.setTextSize(14);
            box.setTextColor(Color.rgb(35,36,40));
            box.setPadding(dp(10), dp(11), dp(10), dp(11));
            box.setOnCheckedChangeListener((button, checked) -> {
                if (checked && !selected.contains(app.packageName)) selected.add(app.packageName);
                else if (!checked) selected.remove(app.packageName);
                updateSelectionUi();
            });
            packageList.addView(box);
            found++;
        }

        if (found == 0) {
            TextView empty = text(
                "No obvious removable bloatware was found.\n\nTry scanning again after OEM or carrier apps finish installing.",
                15, Color.rgb(92,94,100), Typeface.NORMAL
            );
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(35), dp(18), dp(35));
            packageList.addView(empty);
        }

        count.setText(found + " candidate" + (found == 1 ? "" : "s") + " found");
        updateShizukuStatus();
    }

    private void updateSelectionUi() {
        debloatButton.setEnabled(!selected.isEmpty());
        debloatButton.setText(selected.isEmpty()
            ? "DEBLOAT SELECTED"
            : "DEBLOAT " + selected.size() + " SELECTED");
    }

    private void confirmDebloat() {
        if (selected.isEmpty()) return;
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Start Shizuku first.", Toast.LENGTH_LONG).show();
            updateShizukuStatus();
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            requestShizukuPermission();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Disable selected apps?")
            .setMessage(
                "de.bloat will disable the selected packages for the current user. " +
                "It will not delete their APK files, and you can re-enable them later."
            )
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
        updateShizukuStatus();
    }

    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }
}
