package com.example.mesaitakip;

import android.webkit.WebView;

public class JsLoader implements Runnable {
    private final WebView webView;
    private final String js;

    public JsLoader(WebView webView, String js) {
        this.webView = webView;
        this.js = js;
    }

    @Override
    public void run() {
        if (webView != null) {
            webView.loadUrl("javascript:" + js);
        }
    }
}
