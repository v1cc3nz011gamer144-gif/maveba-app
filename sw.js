// Guarda solo la "portada" para que abra rápido; la agenda siempre se carga en vivo desde Google.
var CACHE = "maveba-v2";
var ARCHIVOS = ["./", "./index.html", "./manifest.webmanifest", "./icon-192.png", "./icon-512.png"];
self.addEventListener("install", function(e){ e.waitUntil(caches.open(CACHE).then(function(c){ return c.addAll(ARCHIVOS); })); self.skipWaiting(); });
self.addEventListener("activate", function(e){ e.waitUntil(caches.keys().then(function(ks){ return Promise.all(ks.filter(function(k){return k!==CACHE;}).map(function(k){return caches.delete(k);})); })); self.clients.claim(); });
self.addEventListener("fetch", function(e){
  var u = new URL(e.request.url);
  if (u.origin !== location.origin) return; // Google se carga normal
  e.respondWith(fetch(e.request).catch(function(){ return caches.match(e.request).then(function(r){ return r || caches.match("./index.html"); }); }));
});
