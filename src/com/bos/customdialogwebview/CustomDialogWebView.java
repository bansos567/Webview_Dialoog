package com.bos.customdialogwebview;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

@DesignerComponent(version = 1, description = "Dialog WebView Fullscreen Anti-Scroll & No Zoom", category = com.google.appinventor.components.common.ComponentCategory.EXTENSION, nonVisible = true, iconName = "images/extension.png")
@SimpleObject(external = true)
public class CustomDialogWebView extends AndroidNonvisibleComponent {
    private Context context;
    private Dialog dialog;
    private WebView webView;
    private String webViewString = "";
    private String customUserAgent = "";

    public CustomDialogWebView(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
    }

    @SimpleFunction(description = "Tampilkan Dialog Fullscreen dan Load URL")
    public void ShowDialog(String url) {
        // 1. Setup Dialog Full Screen + Immersive Mode (Tanpa Status Bar / Header)
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

        // --- MATIKAN ZOOM ---
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Set Custom User Agent
        if (!customUserAgent.isEmpty()) {
            settings.setUserAgentString(customUserAgent);
        }

        // 3. Fitur Anti-Scroll (Blokir geser vertikal/horizontal)
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Return true kalau action-nya move (menggagalkan scroll)
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        // 4. Inject Javascript Interface ("Android")
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // Set view ke dialog
        dialog.setContentView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

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
        // Dijalankan di UI Thread biar aman buat Kodular
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventDispatcher.dispatchEvent(CustomDialogWebView.this, "SinyalDiterima", data);
            }
        });
    }

    // Class untuk menjembatani Javascript (Web) ke Java (Kodular)
    private class WebAppInterface {
        
        // Fungsi JS yang dipanggil pakai: Android.KirimSinyal("data");
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
