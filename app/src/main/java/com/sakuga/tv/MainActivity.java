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
import android.view.ViewGroup;
import android.view.WindowManager;
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
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        videoView = new VideoView(this);
        videoView.setFocusable(false);
        videoView.setFocusableInTouchMode(false);
        root.addView(videoView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        ));

        videoView.setOnPreparedListener(mp -> {
            mp.setVolume(0f, 0f);
            mp.setLooping(loopSingle);
            mp.setVideoScalingMode(
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            );
            int vw = mp.getVideoWidth(), vh = mp.getVideoHeight();
            int sw = root.getWidth(), sh = root.getHeight();
            if (vw > 0 && vh > 0 && sw > 0 && sh > 0) {
                float sc = Math.max((float) sw / vw, (float) sh / vh);
                ViewGroup.LayoutParams lp = videoView.getLayoutParams();
                lp.width = Math.round(vw * sc);
                lp.height = Math.round(vh * sc);
                videoView.setLayoutParams(lp);
            }
            videoView.start();
        });

        videoView.setOnCompletionListener(mp -> {
            if (!loopSingle) {
                webView.evaluateJavascript("onVideoEnded();", null);
            }
        });

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
        if (videoView != null && !videoView.isPlaying()) videoView.start();
        if (webView != null) webView.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK &&
            e.getAction() == KeyEvent.ACTION_DOWN) {
            webView.evaluateJavascript("onHardwareBack();", null);
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    public class Bridge {
        @JavascriptInterface
        public void playVideo(String name, boolean loop) {
            runOnUiThread(() -> {
                loopSingle = loop;
                File f = new File("/sdcard/SakugaTV/" + name);
                if (f.exists()) videoView.setVideoURI(Uri.fromFile(f));
                else webView.evaluateJavascript("onVideoEnded();", null);
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
            Intent m = new Intent(Intent.ACTION_MAIN, null);
            m.addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo r : pm.queryIntentActivities(m, 0)) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("name", r.loadLabel(pm).toString());
                    o.put("pkg", r.activityInfo.packageName);
                    arr.put(o);
                } catch (Exception ex) {}
            }
            return arr.toString();
        }
    }
}

