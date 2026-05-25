package com.bos.customdialogwebview;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

@DesignerComponent(version = 3, description = "Dialog WebView Cerdas: Tombol Close Muncul Saat Pindah URL", category = com.google.appinventor.components.common.ComponentCategory.EXTENSION, nonVisible = true, iconName = "images/extension.png")
@SimpleObject(external = true)
public class CustomDialogWebView extends AndroidNonvisibleComponent {
    private Context context;
    private Dialog dialog;
    private WebView webView;
    private String webViewString = "";
    private String customUserAgent = "";
    
    // Variabel baru untuk fitur tombol cerdas
    private TextView closeBtn;
    private String defaultUrl = "";

    public CustomDialogWebView(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
    }

    @SimpleFunction(description = "Tampilkan Dialog Fullscreen dan Load URL")
    public void ShowDialog(String url) {
        // Simpan URL awal sebagai patokan (default)
        this.defaultUrl = url;

        // 1. Setup Dialog Full Screen + Immersive Mode
        dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        // 2. Setup WebView
        webView = new WebView(context);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        if (!customUserAgent.isEmpty()) {
            settings.setUserAgentString(customUserAgent);
        }

        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        // 3. Setup Layout dan Tombol
        FrameLayout mainLayout = new FrameLayout(context);
        FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mainLayout.addView(webView, webViewParams);

        // Bikin Tombol Close
        closeBtn = new TextView(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(24);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setBackgroundColor(Color.parseColor("#4D000000")); 
        closeBtn.setPadding(35, 15, 35, 15);
        
        // SETTING CERDAS: Sembunyikan tombol di awal
        closeBtn.setVisibility(View.GONE);

        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        btnParams.gravity = Gravity.TOP | Gravity.RIGHT;
        btnParams.setMargins(0, 40, 40, 0); 
        closeBtn.setLayoutParams(btnParams);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloseDialog();
            }
        });

        mainLayout.addView(closeBtn);
        dialog.setContentView(mainLayout);

        // 4. Logika Kecerdasan Pindah URL (WebViewClient)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String currentUrl, Bitmap favicon) {
                super.onPageStarted(view, currentUrl, favicon);
                
                if (currentUrl != null && defaultUrl != null) {
                    // Bersihkan slash (garis miring) di ujung URL biar perbandingannya akurat
                    String cleanCurrent = currentUrl.replaceAll("/$", "");
                    String cleanDefault = defaultUrl.replaceAll("/$", "");
                    
                    // Kalau URL saat ini BEDA dengan URL awal
                    if (!cleanCurrent.equals(cleanDefault)) {
                        closeBtn.setVisibility(View.VISIBLE); // Munculkan tombol
                    } else {
                        closeBtn.setVisibility(View.GONE); // Sembunyikan kalau balik ke URL awal
                    }
                }
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // Load URL
        webView.loadUrl(url);
        dialog.show();
    }

    @SimpleFunction(description = "Tutup Dialog Webview")
    public void CloseDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @SimpleProperty(description = "Ubah User Agent WebView")
    public void SetUserAgent(String userAgent) {
        this.customUserAgent = userAgent;
        if (webView != null) {
            webView.getSettings().setUserAgentString(userAgent);
        }
    }

    @SimpleProperty(description = "Ambil nilai WebViewString")
    public String WebViewString() {
        return webViewString;
    }

    @SimpleProperty(description = "Set WebViewString")
    public void WebViewString(String value) {
        this.webViewString = value;
    }

    @SimpleEvent(description = "Terpicu saat Web mengirim sinyal melalui Android.KirimSinyal.")
    public void SinyalDiterima(final String data) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventDispatcher.dispatchEvent(CustomDialogWebView.this, "SinyalDiterima", data);
            }
        });
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void KirimSinyal(String data) {
            SinyalDiterima(data);
        }

        @JavascriptInterface
        public void setWebViewString(String value) {
            webViewString = value;
        }
        
        @JavascriptInterface
        public String getWebViewString() {
            return webViewString;
        }
    }
}
