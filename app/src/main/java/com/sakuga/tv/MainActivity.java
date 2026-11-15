package com.sakuga.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.VideoView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.List;

public class MainActivity extends Activity {
    private VideoView videoView;
    private WebView webView;
    private boolean loopSingle = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        // 1. Нижний слой: видеоплеер БЕЗ перехвата фокуса пульта
        videoView = new VideoView(this);
        videoView.setFocusable(false);
        videoView.setFocusableInTouchMode(false);
        FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        );
        root.addView(videoView, vp);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setVolume(0f, 0f);
                mp.setLooping(loopSingle);
                videoView.start();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (!loopSingle) {
                    webView.evaluateJavascript("onVideoEnded();", null);
                }
            }
        });

        // 2. Верхний слой: интерфейс с принудительным фокусом
        webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        webView.addJavascriptInterface(new Bridge(), "AndroidLauncher");
        webView.loadUrl("file:///android_asset/index.html");
        root.addView(webView);

        setContentView(root);
        webView.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null && !videoView.isPlaying()) {
            videoView.start();
        }
        if (webView != null) {
            webView.requestFocus();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK &&
            event.getAction() == KeyEvent.ACTION_DOWN) {
            webView.evaluateJavascript("onHardwareBack();", null);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public class Bridge {
        @JavascriptInterface
        public void playVideo(final String fileName, final boolean loop) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loopSingle = loop;
                    File f = new File("/sdcard/SakugaTV/" + fileName);
                    if (f.exists()) {
                        videoView.setVideoURI(Uri.fromFile(f));
                    }
                }
            });
        }

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
