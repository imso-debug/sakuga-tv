package com.sakuga.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        WebView wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        wv.addJavascriptInterface(new Bridge(), "AndroidLauncher");
        wv.loadUrl("file:///android_asset/index.html");
        setContentView(wv);
    }

    @Override
    public void onBackPressed() {}

    public class Bridge {
        @JavascriptInterface
        public void openApp(String pkg) {
            if ("settings".equals(pkg)) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                return;
            }
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) startActivity(i);
        }

        @JavascriptInterface
        public String getApps() {
            JSONArray arr = new JSONArray();
            PackageManager pm = getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN, null);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
            for (ResolveInfo r : list) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("name", r.loadLabel(pm).toString());
                    o.put("pkg", r.activityInfo.packageName);
                    arr.put(o);
                } catch (Exception e) {}
            }
            return arr.toString();
        }
    }
}
