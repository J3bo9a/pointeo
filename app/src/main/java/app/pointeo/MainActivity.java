package app.pointeo;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html?splash");
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    private class Bridge {
        @JavascriptInterface
        public void share(String b64, String name, String mime, String text) {
            try {
                File dir = new File(getCacheDir(), "shared");
                dir.mkdirs();
                File f = new File(dir, name);
                try (FileOutputStream o = new FileOutputStream(f)) {
                    o.write(Base64.decode(b64, Base64.DEFAULT));
                }
                final Uri uri = FileProvider.getUriForFile(MainActivity.this, "app.pointeo.files", f);
                final Intent send = new Intent(Intent.ACTION_SEND);
                send.setType(mime);
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.putExtra(Intent.EXTRA_TEXT, text);
                send.putExtra(Intent.EXTRA_SUBJECT, name);
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(Intent.createChooser(send, null)));
            } catch (Exception e) {
                toast("Erreur : " + e.getMessage());
            }
        }

        @JavascriptInterface
        public void save(String b64, String name, String mime) {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, mime);
                v.put(MediaStore.Downloads.RELATIVE_PATH, "Download/Pointeo");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                try (OutputStream o = getContentResolver().openOutputStream(uri)) {
                    o.write(Base64.decode(b64, Base64.DEFAULT));
                }
                toast("Enregistré dans Téléchargements/Pointeo");
            } catch (Exception e) {
                toast("Erreur : " + e.getMessage());
            }
        }

        @JavascriptInterface
        public void print() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                pm.print("Pointeo", web.createPrintDocumentAdapter("Pointeo"), new PrintAttributes.Builder().build());
            });
        }
    }

    private void toast(final String m) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
    }
}
