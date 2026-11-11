package pe.maveba.agenda;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    // Portada de la app (GitHub Pages). Ahí está guardado el link de cada chica.
    static final String INICIO = "https://v1cc3nz011gamer144-gif.github.io/maveba-app/";
    static final String DESCARGA_APK = "https://github.com/v1cc3nz011gamer144-gif/maveba-app/releases/latest/download/MaVeBa.apk";

    static final int NEGRO = Color.parseColor("#000000");
    static final int DORADO = Color.parseColor("#E0B04F");
    static final int CREMA = Color.parseColor("#FAF5EB");
    static final int BARRA = Color.parseColor("#FBF8F3");      // fondo de la barra (crema como la agenda)
    static final int LINEA = Color.parseColor("#E9E0D2");      // línea fina arriba de la barra
    static final int CAFE = Color.parseColor("#5B4A3B");       // íconos y textos
    static final int TOQUE = Color.parseColor("#33C9A24E");    // brillo dorado al tocar
    static final int REQ_ARCHIVO = 11;
    static final int REQ_PERMISO = 12;

    WebView web;
    LinearLayout capa;      // pantalla de carga / sin internet
    TextView capaTexto;
    Button capaBoton;
    ValueCallback<Uri[]> esperandoArchivo;
    String[] pendienteGuardar; // nombre, tipo, base64 (si falta permiso en Android viejo)
    boolean errorPrincipal = false;

    // Se mete dentro de la agenda (también en sus marcos internos) para que funcionen
    // "Descargar PDF" y los botones de WhatsApp dentro de la app.
    static final String PUENTE_JS =
        "(function(){if(window.__mvb)return;window.__mvb=1;var B=window.MaVeBaApp;if(!B)return;" +
        "function env(o){try{B.postMessage(JSON.stringify(o));}catch(e){}}" +
        "var blobs={};var oc=URL.createObjectURL;URL.createObjectURL=function(o){var u=oc.apply(URL,arguments);try{if(o instanceof Blob)blobs[u]=o;}catch(e){}return u;};" +
        "function porBlob(bl,n){var r=new FileReader();r.onload=function(){var s=String(r.result);env({t:'guardar',n:n,m:bl.type||'',d:s.slice(s.indexOf(',')+1)});};r.readAsDataURL(bl);}" +
        "function guardar(a){var h=a.href||'',n=a.getAttribute('download')||'archivo';" +
        " if(h.indexOf('blob:')===0){var b=blobs[h];if(b)porBlob(b,n);else fetch(h).then(function(x){return x.blob();}).then(function(x){porBlob(x,n);});return true;}" +
        " if(h.indexOf('data:')===0){var c=h.indexOf(','),meta=h.slice(5,c),d=h.slice(c+1);if(!/;base64/.test(meta)){d=btoa(unescape(encodeURIComponent(decodeURIComponent(d))));}env({t:'guardar',n:n,m:meta.split(';')[0],d:d});return true;}" +
        " return false;}" +
        "function esDescarga(a){return a&&a.tagName==='A'&&a.hasAttribute('download');}" +
        "function esFuera(u){return /^(https?:\\/\\/(wa\\.me|api\\.whatsapp\\.com|web\\.whatsapp\\.com|maps\\.|www\\.google\\.com\\/maps|drive\\.google\\.com|docs\\.google\\.com)|whatsapp:|tel:|mailto:|intent:)/i.test(u||'');}" +
        "var ck=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){if(esDescarga(this)&&guardar(this))return;if(esFuera(this.href)){env({t:'abrir',u:this.href});return;}return ck.apply(this,arguments);};" +
        "var de=EventTarget.prototype.dispatchEvent;EventTarget.prototype.dispatchEvent=function(ev){if(ev&&ev.type==='click'&&esDescarga(this)&&guardar(this))return false;return de.apply(this,arguments);};" +
        "document.addEventListener('click',function(e){var a=e.target&&e.target.closest&&e.target.closest('a');if(!a)return;if(esDescarga(a)&&guardar(a)){e.preventDefault();e.stopPropagation();return;}if(esFuera(a.href)){e.preventDefault();e.stopPropagation();env({t:'abrir',u:a.href});}},true);" +
        "var wo=window.open;window.open=function(u){if(esFuera(u)){env({t:'abrir',u:String(u)});return null;}return wo.apply(window,arguments);};" +
        "})();";

    // Esconde el aviso gris de Google ("Esta aplicación fue creada por un usuario...")
    static final String QUITAR_AVISO_JS = "(function(){ if(location.hostname!=='script.google.com'||window.__mvbAviso)return;window.__mvbAviso=1; var RX=/(Apps Script|creada por (otro|un) usuario|created by (another|a Google)|no por Google|not by Google|Informar sobre|Report abuse)/i; var css=document.createElement('style'); css.textContent='html,body{margin:0!important;padding:0!important;overflow:hidden!important;background:#fff!important}'; function arreglar(){ var hecho=false; if(!document.body)return false; if(!css.parentNode)(document.head||document.documentElement).appendChild(css); var todos=document.body.querySelectorAll('div,table,tr,td,span,p,a'); for(var i=0;i<todos.length;i++){ var el=todos[i]; if(el.querySelector('iframe'))continue; var t=el.textContent||''; if(t.length<400&&RX.test(t)){ var c=el; while(c.parentElement&&c.parentElement!==document.body&&!c.parentElement.querySelector('iframe'))c=c.parentElement; if(c.style.display!=='none'){c.style.setProperty('display','none','important');hecho=true;} } } var fr=document.querySelectorAll('iframe'); for(var j=0;j<fr.length;j++){ var f=fr[j]; if(f.offsetWidth<50&&f.offsetHeight<50)continue; f.style.setProperty('position','fixed','important');f.style.setProperty('top','0','important'); f.style.setProperty('left','0','important');f.style.setProperty('width','100%','important'); f.style.setProperty('height','100%','important');f.style.setProperty('border','0','important'); f.style.setProperty('z-index','2147483000','important'); } return hecho; } var mo=new MutationObserver(function(){arreglar();}); function iniciar(){arreglar();try{mo.observe(document.documentElement,{childList:true,subtree:true});}catch(e){}setTimeout(function(){mo.disconnect();},15000);} if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',iniciar);else iniciar(); })();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pintarBarrasSistema(NEGRO, false);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(NEGRO);

        FrameLayout marco = new FrameLayout(this);
        raiz.addView(marco, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        web = new WebView(this);
        marco.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        capa = crearCapa();
        marco.addView(capa, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View linea = new View(this);
        linea.setBackgroundColor(LINEA);
        raiz.addView(linea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2)));
        raiz.addView(crearBarra(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
        setContentView(raiz);

        configurarWeb();
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else abrirInicio("");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    // ---------- Pantalla de carga / sin internet ----------
    LinearLayout crearCapa() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setBackgroundColor(NEGRO);
        c.setPadding(dp(24), dp(24), dp(24), dp(24));
        c.setClickable(true);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        logo.setAdjustViewBounds(true);
        c.addView(logo, new LinearLayout.LayoutParams(dp(180), dp(180)));

        capaTexto = new TextView(this);
        capaTexto.setTextColor(CREMA);
        capaTexto.setTextSize(17);
        capaTexto.setGravity(Gravity.CENTER);
        capaTexto.setTypeface(Typeface.SERIF);
        capaTexto.setPadding(0, dp(16), 0, dp(16));
        capaTexto.setText("Abriendo tu agenda…");
        c.addView(capaTexto);

        capaBoton = botonDorado("Reintentar");
        capaBoton.setVisibility(View.GONE);
        capaBoton.setOnClickListener(v -> abrirInicio(""));
        c.addView(capaBoton);
        return c;
    }

    Button botonDorado(String t) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(NEGRO);
        b.setBackgroundColor(DORADO);
        b.setPadding(dp(28), dp(10), dp(28), dp(10));
        return b;
    }

    void mostrarCargando() {
        pintarBarrasSistema(NEGRO, false);
        capaTexto.setText("Abriendo tu agenda…");
        capaBoton.setVisibility(View.GONE);
        capa.setVisibility(View.VISIBLE);
    }

    void mostrarSinInternet() {
        pintarBarrasSistema(NEGRO, false);
        capaTexto.setText("Sin internet\n\nLa agenda necesita internet.\nRevisa tu wifi o tus datos y vuelve a intentar.");
        capaBoton.setVisibility(View.VISIBLE);
        capa.setVisibility(View.VISIBLE);
    }

    // ---------- Barras del sistema (hora/batería arriba y botones de Android abajo) ----------
    void pintarBarrasSistema(int arriba, boolean clara) {
        getWindow().setStatusBarColor(arriba);
        getWindow().setNavigationBarColor(clara ? BARRA : NEGRO);
        View d = getWindow().getDecorView();
        int f = d.getSystemUiVisibility();
        if (clara) {
            f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) f &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        d.setSystemUiVisibility(f);
    }

    // ---------- Barra de abajo ----------
    LinearLayout crearBarra() {
        LinearLayout barra = new LinearLayout(this);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setBackgroundColor(BARRA);
        barra.setPadding(dp(6), 0, dp(6), 0);
        barra.addView(botonBarra(Icono.ATRAS, "Atrás", v -> atras()));
        barra.addView(botonBarra(Icono.INICIO, "Inicio", v -> abrirInicio("")));
        barra.addView(botonBarra(Icono.RECARGAR, "Recargar", v -> recargar()));
        barra.addView(botonBarra(Icono.MAS, "Más", v -> menuMas()));
        return barra;
    }

    LinearLayout botonBarra(int tipo, String texto, View.OnClickListener l) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(l);
        b.setContentDescription(texto);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(TOQUE), null, new ColorDrawable(Color.WHITE)));
        b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        Icono ic = new Icono(this, tipo, CAFE);
        b.addView(ic, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView t = new TextView(this);
        t.setText(texto);
        t.setTextColor(CAFE);
        t.setTextSize(11);
        t.setLetterSpacing(0.02f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(3), 0, 0);
        b.addView(t);
        return b;
    }

    // Íconos de línea finos, dibujados a mano (sin imágenes)
    static class Icono extends View {
        static final int ATRAS = 0, INICIO = 1, RECARGAR = 2, MAS = 3;
        final int tipo;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();

        Icono(Context c, int tipo, int color) {
            super(c);
            this.tipo = tipo;
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onDraw(Canvas c) {
            float u = getWidth() / 24f;           // dibujo en una cuadrícula de 24x24
            p.setStrokeWidth(1.8f * u);
            p.setStyle(Paint.Style.STROKE);
            path.reset();
            if (tipo == ATRAS) {
                path.moveTo(14.5f * u, 5.5f * u);
                path.lineTo(8f * u, 12f * u);
                path.lineTo(14.5f * u, 18.5f * u);
                c.drawPath(path, p);
            } else if (tipo == INICIO) {
                path.moveTo(4f * u, 11f * u);
                path.lineTo(12f * u, 4.5f * u);
                path.lineTo(20f * u, 11f * u);
                c.drawPath(path, p);
                path.reset();
                path.moveTo(6.5f * u, 9.5f * u);
                path.lineTo(6.5f * u, 19.5f * u);
                path.lineTo(17.5f * u, 19.5f * u);
                path.lineTo(17.5f * u, 9.5f * u);
                c.drawPath(path, p);
                path.reset();
                path.moveTo(10.25f * u, 19.5f * u);
                path.lineTo(10.25f * u, 14.5f * u);
                path.lineTo(13.75f * u, 14.5f * u);
                path.lineTo(13.75f * u, 19.5f * u);
                c.drawPath(path, p);
            } else if (tipo == RECARGAR) {
                RectF r = new RectF(5f * u, 5f * u, 19f * u, 19f * u);
                c.drawArc(r, -30f, 300f, false, p);     // círculo abierto arriba a la derecha
                path.moveTo(9.8f * u, 2.2f * u);          // punta de flecha arriba
                path.lineTo(12.8f * u, 5f * u);
                path.lineTo(9.8f * u, 7.8f * u);
                c.drawPath(path, p);
            } else {
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(5.5f * u, 12f * u, 1.6f * u, p);
                c.drawCircle(12f * u, 12f * u, 1.6f * u, p);
                c.drawCircle(18.5f * u, 12f * u, 1.6f * u, p);
            }
        }
    }

    void menuMas() {
        String[] op = {"Recargar la agenda", "Cambiar de usuaria", "Buscar versión nueva de la app"};
        new AlertDialog.Builder(this)
            .setTitle("MaVeBa")
            .setItems(op, (d, i) -> {
                if (i == 0) recargar();
                else if (i == 1) confirmarCambio();
                else abrirFuera(DESCARGA_APK);
            })
            .setNegativeButton("Cerrar", null)
            .show();
    }

    void confirmarCambio() {
        new AlertDialog.Builder(this)
            .setTitle("Cambiar de usuaria")
            .setMessage("Se olvidará el link guardado en este celular y tendrás que pegar el link personal de la otra chica. ¿Continuar?")
            .setPositiveButton("Sí, cambiar", (d, i) -> abrirInicio("?cambiar"))
            .setNegativeButton("No", null)
            .show();
    }

    void abrirInicio(String extra) {
        if (!hayInternet()) { mostrarSinInternet(); return; }
        errorPrincipal = false;
        mostrarCargando();
        web.loadUrl(INICIO + extra);
    }

    void recargar() {
        if (!hayInternet()) { mostrarSinInternet(); return; }
        errorPrincipal = false;
        mostrarCargando();
        web.reload();
    }

    void atras() {
        if (capa.getVisibility() == View.VISIBLE && errorPrincipal) { abrirInicio(""); return; }
        if (web.canGoBack()) { web.goBack(); return; }
        new AlertDialog.Builder(this)
            .setMessage("¿Salir de MaVeBa?")
            .setPositiveButton("Salir", (d, i) -> finish())
            .setNegativeButton("Quedarme", null)
            .show();
    }

    @Override
    public void onBackPressed() { atras(); }

    boolean hayInternet() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo n = cm.getActiveNetworkInfo();
            return n != null && n.isConnected();
        } catch (Exception e) { return true; }
    }

    // ---------- El navegador interno ----------
    void configurarWeb() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setBackgroundColor(NEGRO);

        Set<String> origenes = new HashSet<>(Arrays.asList(
            "https://v1cc3nz011gamer144-gif.github.io",
            "https://script.google.com",
            "https://*.googleusercontent.com"));

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(web, "MaVeBaApp", origenes,
                (view, mensaje, origen, esPrincipal, respuesta) -> recibir(mensaje.getData()));
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try { WebViewCompat.addDocumentStartJavaScript(web, PUENTE_JS, origenes); } catch (Throwable ignored) {}
            try { WebViewCompat.addDocumentStartJavaScript(web, QUITAR_AVISO_JS, new HashSet<>(Arrays.asList("https://script.google.com"))); } catch (Throwable ignored) {}
        }

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String esq = u.getScheme() == null ? "" : u.getScheme();
                if (!esq.equals("http") && !esq.equals("https")) { abrirFuera(u.toString()); return true; }
                if (r.isForMainFrame() && !esNuestro(u)) { abrirFuera(u.toString()); return true; }
                if (esWhatsApp(u)) { abrirFuera(u.toString()); return true; }
                return false;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                if (!errorPrincipal) { capa.setVisibility(View.GONE); pintarBarrasSistema(BARRA, true); }
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) { errorPrincipal = true; mostrarSinInternet(); }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView v, boolean dialogo, boolean gesto, android.os.Message msg) {
                // Ventanas nuevas (por ejemplo WhatsApp): se abren fuera, en su app.
                WebView temporal = new WebView(MainActivity.this);
                temporal.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView t, WebResourceRequest r) {
                        abrirFuera(r.getUrl().toString());
                        t.destroy();
                        return true;
                    }
                });
                WebView.WebViewTransport tr = (WebView.WebViewTransport) msg.obj;
                tr.setWebView(temporal);
                msg.sendToTarget();
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (esperandoArchivo != null) esperandoArchivo.onReceiveValue(null);
                esperandoArchivo = cb;
                try {
                    Intent i = p.createIntent();
                    if (p.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(Intent.createChooser(i, "Elegir archivo"), REQ_ARCHIVO);
                } catch (Exception e) {
                    esperandoArchivo = null;
                    aviso("No se pudo abrir la galería");
                    return false;
                }
                return true;
            }
        });

        web.setDownloadListener((url, ua, disp, mime, largo) -> {
            if (url.startsWith("http")) abrirFuera(url);
        });
    }

    boolean esNuestro(Uri u) {
        String h = u.getHost() == null ? "" : u.getHost();
        return h.equals("v1cc3nz011gamer144-gif.github.io")
            || h.equals("script.google.com")
            || h.endsWith(".googleusercontent.com")
            || h.equals("accounts.google.com");
    }

    boolean esWhatsApp(Uri u) {
        String h = u.getHost() == null ? "" : u.getHost();
        return h.equals("wa.me") || h.endsWith("whatsapp.com");
    }

    void abrirFuera(String url) {
        try {
            Intent i;
            if (url.startsWith("intent:")) i = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            else i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            aviso("No hay una app para abrir esto");
        } catch (Exception e) {
            aviso("No se pudo abrir el enlace");
        }
    }

    void aviso(String t) { Toast.makeText(this, t, Toast.LENGTH_LONG).show(); }

    // ---------- Mensajes que manda la agenda ----------
    void recibir(String datos) {
        try {
            JSONObject o = new JSONObject(datos);
            String t = o.optString("t");
            if (t.equals("abrir")) abrirFuera(o.optString("u"));
            else if (t.equals("guardar")) guardarArchivo(o.optString("n", "archivo"), o.optString("m", ""), o.optString("d", ""));
        } catch (Exception e) {
            aviso("No se pudo completar la acción");
        }
    }

    void guardarArchivo(String nombre, String tipo, String b64) {
        if (tipo == null || tipo.isEmpty()) tipo = nombre.toLowerCase().endsWith(".pdf") ? "application/pdf" : "application/octet-stream";
        nombre = nombre.replaceAll("[\\\\/:*?\"<>|]", "_");
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            Uri abrir = null;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, nombre);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, tipo);
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) throw new Exception("sin uri");
                try (OutputStream os = getContentResolver().openOutputStream(uri)) { os.write(bytes); }
                abrir = uri;
            } else {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    pendienteGuardar = new String[]{nombre, tipo, b64};
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERMISO);
                    return;
                }
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, nombre);
                try (FileOutputStream fo = new FileOutputStream(f)) { fo.write(bytes); }
            }
            aviso("Guardado en Descargas: " + nombre);
            if (abrir != null) {
                try {
                    Intent v = new Intent(Intent.ACTION_VIEW);
                    v.setDataAndType(abrir, tipo);
                    v.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(v);
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            aviso("No se pudo guardar el archivo");
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req == REQ_PERMISO && pendienteGuardar != null) {
            String[] p = pendienteGuardar;
            pendienteGuardar = null;
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) guardarArchivo(p[0], p[1], p[2]);
            else aviso("Sin permiso no se puede guardar en Descargas");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_ARCHIVO || esperandoArchivo == null) return;
        Uri[] r = null;
        if (res == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                r = new Uri[n];
                for (int i = 0; i < n; i++) r[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) {
                r = new Uri[]{data.getData()};
            }
        }
        esperandoArchivo.onReceiveValue(r);
        esperandoArchivo = null;
    }
}
