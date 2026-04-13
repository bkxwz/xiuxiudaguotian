package com.xiuxiudaguotian;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST = 1;
    private WebView webView;
    private MediaRecorder mediaRecorder;
    private String currentFilePath;
    private boolean isRecording = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        webView = new WebView(this);
        setContentView(webView);
        
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        
        webView.setWebChromeClient(new WebChromeClient());
        
        String html = loadHtmlFromAssets();
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        
        requestPermissions();
    }
    
    private String loadHtmlFromAssets() {
        try {
            java.io.InputStream is = getAssets().open("index.html");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    
    private class AndroidBridge {
        @JavascriptInterface
        public void startRecording() {
            if (isRecording) return;
            
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File audioDir = new File(getFilesDir(), "recordings");
                        if (!audioDir.exists()) {
                            audioDir.mkdirs();
                        }
                        currentFilePath = new File(audioDir, "recording_" + System.currentTimeMillis() + ".mp3").getAbsolutePath();
                        
                        mediaRecorder = new MediaRecorder();
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mediaRecorder.setOutputFile(currentFilePath);
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        isRecording = true;
                        
                        // Auto-stop after 7 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (isRecording && mediaRecorder != null) {
                                    try {
                                        mediaRecorder.stop();
                                        mediaRecorder.release();
                                        mediaRecorder = null;
                                        isRecording = false;
                                        String fileUrl = "file://" + currentFilePath;
                                        final String escapedUrl = fileUrl.replace("\\", "\\\\").replace("'", "\\'");
                                        webView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                webView.evaluateJavascript("onRecordingAutoStop('" + escapedUrl + "')", null);
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }, 7000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        webView.post(new Runnable() {
                            @Override
                            public void run() {
                                webView.evaluateJavascript("onRecordingError('录音启动失败: " + e.getMessage() + "')", null);
                            }
                        });
                    }
                }
            });
        }
        
        @JavascriptInterface
        public void stopRecording() {
            if (!isRecording || mediaRecorder == null) return;
            
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mediaRecorder.stop();
                        mediaRecorder.release();
                        mediaRecorder = null;
                        isRecording = false;
                        
                        // Send file path to JS - use file:// URL
                        String fileUrl = "file://" + currentFilePath;
                        webView.post(new Runnable() {
                            @Override
                            public void run() {
                                webView.evaluateJavascript("onRecordingComplete('" + fileUrl.replace("\\", "\\\\").replace("'", "\\'") + "')", null);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        webView.post(new Runnable() {
                            @Override
                            public void run() {
                                webView.evaluateJavascript("onRecordingError('录音停止失败: " + e.getMessage() + "')", null);
                            }
                        });
                    }
                }
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {}
            mediaRecorder.release();
        }
    }
}