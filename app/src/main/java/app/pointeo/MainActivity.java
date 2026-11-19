package app.pointeo;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebViewAssetLoader;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private FrameLayout root;
    private ValueCallback<Uri[]> chooser;
    private String pendingB64;
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        web = new WebView(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        root.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
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

            @Override
            public void onPageFinished(WebView v, String url) {
                loaded = true;
                deliver();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                java.util.ArrayList<String> types = new java.util.ArrayList<>();
                for (String a : p.getAcceptTypes()) {
                    if (a != null && a.contains("/") && !types.contains(a)) types.add(a);
                }
                if (types.isEmpty()) {
                    i.setType("*/*");
                } else if (types.size() == 1) {
                    i.setType(types.get(0));
                } else {
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_MIME_TYPES, types.toArray(new String[0]));
                }
                try {
                    startActivityForResult(Intent.createChooser(i, null), 1);
                } catch (Exception e) {
                    chooser = null;
                    cb.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html?splash");
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent in) {
        super.onNewIntent(in);
        setIntent(in);
        handleIntent(in);
    }

    @Override
    protected void onActivityResult(int rq, int rs, Intent d) {
        if (rq == 1) {
            if (chooser != null) {
                chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(rs, d));
                chooser = null;
            }
            return;
        }
        super.onActivityResult(rq, rs, d);
    }

    private void handleIntent(Intent in) {
        if (in == null) return;
        String a = in.getAction();
        Uri u = null;
        if (Intent.ACTION_VIEW.equals(a)) u = in.getData();
        else if (Intent.ACTION_SEND.equals(a)) u = in.getParcelableExtra(Intent.EXTRA_STREAM);
        if (u == null) return;
        try (InputStream is = getContentResolver().openInputStream(u);
             ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            pendingB64 = Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP);
            deliver();
        } catch (Exception e) {
            toast("Erreur : " + e.getMessage());
        }
    }

    private void deliver() {
        if (pendingB64 != null && loaded) {
            String js = "window.openSheetB64&&window.openSheetB64('" + pendingB64 + "','file');";
            pendingB64 = null;
            runOnUiThread(() -> web.evaluateJavascript(js, null));
        }
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
        public void ocr(final String id, String b64) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bm == null) { ocrDone(id, "null"); return; }
                TextRecognizer rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                rec.process(InputImage.fromBitmap(bm, 0))
                    .addOnSuccessListener(t -> {
                        try {
                            JSONArray lines = new JSONArray();
                            for (Text.TextBlock bl : t.getTextBlocks()) {
                                for (Text.Line ln : bl.getLines()) {
                                    JSONObject o = box(ln.getText(), ln.getBoundingBox());
                                    if (o == null) continue;
                                    JSONArray els = new JSONArray();
                                    for (Text.Element e : ln.getElements()) {
                                        JSONObject eo = box(e.getText(), e.getBoundingBox());
                                        if (eo != null) els.put(eo);
                                    }
                                    o.put("e", els);
                                    lines.put(o);
                                }
                            }
                            ocrDone(id, lines.toString());
                        } catch (Exception ex) {
                            ocrDone(id, "null");
                        } finally {
                            rec.close();
                        }
                    })
                    .addOnFailureListener(e -> { rec.close(); ocrDone(id, "null"); });
            } catch (Throwable e) {
                ocrDone(id, "null");
            }
        }

        @JavascriptInterface
        public void theme(final boolean dark) {
            runOnUiThread(() -> {
                root.setBackgroundColor(dark ? Color.BLACK : Color.WHITE);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
                getWindow().setNavigationBarColor(Color.TRANSPARENT);
                getWindow().setNavigationBarContrastEnforced(false);
                WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                c.setAppearanceLightStatusBars(!dark);
                c.setAppearanceLightNavigationBars(!dark);
            });
        }

        @JavascriptInterface
        public void print() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                pm.print("Pointeo", web.createPrintDocumentAdapter("Pointeo"), new PrintAttributes.Builder().build());
            });
        }
    }


    private static JSONObject box(String text, Rect r) throws Exception {
        if (r == null) return null;
        JSONObject o = new JSONObject();
        o.put("t", text);
        o.put("x", r.left);
        o.put("y", r.top);
        o.put("w", r.width());
        o.put("h", r.height());
        return o;
    }

    private void ocrDone(final String id, final String json) {
        runOnUiThread(() -> web.evaluateJavascript(
            "window.__ocr&&window.__ocr('" + id.replaceAll("[^a-zA-Z0-9]", "") + "'," + json + ")", null));
    }

    private void toast(final String m) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
    }
}
