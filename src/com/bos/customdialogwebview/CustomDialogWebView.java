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
import android.webkit.CookieManager;
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

@DesignerComponent(version = 4, description = "Dialog WebView Pro: Anti White Flash, Cookie/Cache Control, JS Control", category = com.google.appinventor.components.common.ComponentCategory.EXTENSION, nonVisible = true, iconName = "images/extension.png")
@SimpleObject(external = true)
public class CustomDialogWebView extends AndroidNonvisibleComponent {
    private Context context;
    private Dialog dialog;
    private WebView webView;
    
    private String webViewString = "";
    private String customUserAgent = "";
    private String defaultUrl = "";
    private TextView closeBtn;

    // Properti Baru
    private boolean isCancelable = false;
    private boolean isJsEnabled = true;
    private String bgColorHex = "#19222e"; // Default warna gelap

    public CustomDialogWebView(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
    }

    @SimpleFunction(description = "Tampilkan Dialog Fullscreen dan Load URL")
    public void ShowDialog(String url) {
        this.defaultUrl = url;

        // 1. Setup Dialog Full Screen (TAPI TOMBOL NAVIGASI BAWAH TETAP ADA)
        dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Atur Cancelable (Bisa ditutup pakai tombol back atau klik luar layar)
        dialog.setCancelable(isCancelable);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN); // Nyembunyiin Status Bar Atas doang
        }

        // 2. Setup WebView
        webView = new WebView(context);
        
        // Atur Warna Background biar gak kedip putih (Anti White-Flash)
        try {
            webView.setBackgroundColor(Color.parseColor(bgColorHex));
        } catch (Exception e) {
            webView.setBackgroundColor(Color.BLACK); // Fallback kalau kode hex salah
        }

        WebSettings settings = webView.getSettings();
        
        // Kontrol JavaScript
        settings.setJavaScriptEnabled(isJsEnabled);
        
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

        // 3. Setup Layout dan Tombol Floating Cerdas
        FrameLayout mainLayout = new FrameLayout(context);
        FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mainLayout.addView(webView, webViewParams);

        closeBtn = new TextView(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(24);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setBackgroundColor(Color.parseColor("#4D000000")); 
        closeBtn.setPadding(35, 15, 35, 15);
        closeBtn.setVisibility(View.GONE); // Default sembunyi

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

        // 4. Logika Kecerdasan Pindah URL
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String currentUrl, Bitmap favicon) {
                super.onPageStarted(view, currentUrl, favicon);
                if (currentUrl != null && defaultUrl != null) {
                    String cleanCurrent = currentUrl.replaceAll("/$", "");
                    String cleanDefault = defaultUrl.replaceAll("/$", "");
                    
                    if (!cleanCurrent.equals(cleanDefault)) {
                        closeBtn.setVisibility(View.VISIBLE);
                    } else {
                        closeBtn.setVisibility(View.GONE);
                    }
                }
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl(url);
        dialog.show();
    }

    @SimpleFunction(description = "Tutup Dialog Webview")
    public void CloseDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    // --- FUNGSI BARU ---

    @SimpleFunction(description = "Hapus Cache WebView")
    public void ClearCache() {
        if (webView != null) {
            webView.clearCache(true);
        }
    }

    @SimpleFunction(description = "Hapus Cookies WebView")
    public void ClearCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        } else {
            cookieManager.removeAllCookie();
        }
    }

    // --- PROPERTI BARU ---

    @SimpleProperty(description = "Atur warna loading background. Gunakan kode Hex (contoh: #19222e)")
    public void BackgroundColor(String hexColor) {
        this.bgColorHex = hexColor;
    }

    @SimpleProperty(description = "Aktifkan atau Matikan eksekusi JavaScript (true/false)")
    public void EnableJavaScript(boolean enable) {
        this.isJsEnabled = enable;
    }

    @SimpleProperty(description = "Dialog bisa ditutup dengan tombol Back HP? (true/false)")
    public void Cancelable(boolean cancelable) {
        this.isCancelable = cancelable;
    }

    @SimpleProperty(description = "Ubah User Agent WebView")
    public void SetUserAgent(String userAgent) {
        this.customUserAgent = userAgent;
    }

    @SimpleProperty(description = "Ambil nilai WebViewString")
    public String WebViewString() {
        return webViewString;
    }

    @SimpleProperty(description = "Set WebViewString")
    public void WebViewString(String value) {
        this.webViewString = value;
    }

    // --- EVENT ---

    @SimpleEvent(description = "Terpicu saat WebViewString berubah dari sisi Website")
    public void WebViewStringChanged(final String value) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventDispatcher.dispatchEvent(CustomDialogWebView.this, "WebViewStringChanged", value);
            }
        });
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

    // --- JEMBATAN JAVASCRIPT ---
    private class WebAppInterface {
        @JavascriptInterface
        public void KirimSinyal(String data) {
            SinyalDiterima(data);
        }

        @JavascriptInterface
        public void setWebViewString(String value) {
            webViewString = value;
            WebViewStringChanged(value); // Otomatis trigger event ke Kodular
        }
        
        @JavascriptInterface
        public String getWebViewString() {
            return webViewString;
        }
    }
}
