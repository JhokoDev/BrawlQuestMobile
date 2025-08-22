package com.google.mediapipe.examples.poselandmarker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.mediapipe.examples.poselandmarker.model.DungeonType;
import com.google.mediapipe.examples.poselandmarker.objetos.Portal;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Gps extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private static final String MAPTILER_KEY = "OQLaynlp6gA08keMzOwZ";
    private static final String MAPTILER_STYLE = "streets-v2-dark";
    private List<Portal> portalList = new ArrayList<>();
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private final Handler animationHandler = new Handler(Looper.getMainLooper());
    private GeoPoint lastKnownLocation;
    private static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 segundos
    private static final long ANIMATION_FRAME_INTERVAL = 50; // 200ms por frame
    private static final int BASE_ICON_SIZE = 200; // Tamanho padrão do ícone
    private static final double BASE_ZOOM_LEVEL = 18.0; // Zoom base
    private static final String PREFS_NAME = "PortalCache";
    private static final String KEY_PORTAL_CACHE = "portal_cache";
    private static final String KEY_CACHE_TIMESTAMP = "cache_timestamp";
    private static final String KEY_CACHE_LOCATION = "cache_location";
    private static final long CACHE_EXPIRY = 24 * 60 * 60 * 1000; // 24 horas
    private static final double CACHE_LOCATION_THRESHOLD = 5000; // 5 km
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuração inicial do OSMDroid
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_gps);

        // Inicializar o mapa
        mapView = findViewById(R.id.mapFragment);
        mapView.setMultiTouchControls(true);

        // Configurar fonte de tiles MapTiler
        OnlineTileSourceBase mapTilerSource = new OnlineTileSourceBase(
                "MapTilerRaster", 0, 22, 256, ".png",
                new String[]{"https://api.maptiler.com/maps/" + MAPTILER_STYLE + "/"}
        ) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                String url = getBaseUrl()
                        + MapTileIndex.getZoom(pMapTileIndex) + "/"
                        + MapTileIndex.getX(pMapTileIndex) + "/"
                        + MapTileIndex.getY(pMapTileIndex)
                        + ".png?key=" + MAPTILER_KEY;
                Log.d("MapTiler", "Tile URL: " + url);
                return url;
            }
        };

        // Fallback para OpenStreetMap
        OnlineTileSourceBase fallbackSource = new XYTileSource(
                "OSMFallback", 0, 20, 256, ".png",
                new String[]{"https://tile.openstreetmap.org/"}
        );

        try {
            mapView.setTileSource(mapTilerSource);
        } catch (Exception e) {
            Log.e("MapTiler", "Falha ao carregar MapTiler, usando fallback", e);
            mapView.setTileSource(fallbackSource);
            Toast.makeText(this, "Erro ao carregar tiles do MapTiler, usando mapa padrão", Toast.LENGTH_LONG).show();
        }

        // Configurar controlador do mapa
        IMapController mapController = mapView.getController();
        mapController.setZoom(18.0);
        mapController.setCenter(new GeoPoint(0.0, 0.0));

        // Configurar overlay de localização
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (myLocationOverlay.getMyLocation() != null) {
                lastKnownLocation = myLocationOverlay.getMyLocation();
                Log.d("GPS", "Localização inicial: lat=" + lastKnownLocation.getLatitude() + ", lon=" + lastKnownLocation.getLongitude());
                mapController.animateTo(lastKnownLocation);
                loadPortalsFromOverpass(lastKnownLocation);
                Toast.makeText(Gps.this, "Localização encontrada", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("GPS", "Localização inicial não disponível");
                Toast.makeText(Gps.this, "Localização não encontrada", Toast.LENGTH_SHORT).show();
            }
        }));
        mapView.getOverlays().add(myLocationOverlay);

        // Iniciar atualizações de localização
        startLocationUpdates();

        // Botão para centralizar na localização
        Button btnCenterLocation = findViewById(R.id.btnCenterLocation);
        btnCenterLocation.setOnClickListener(v -> {
            if (myLocationOverlay.getMyLocation() != null) {
                lastKnownLocation = myLocationOverlay.getMyLocation();
                Log.d("GPS", "Centralizando em: lat=" + lastKnownLocation.getLatitude() + ", lon=" + lastKnownLocation.getLongitude());
                mapController.animateTo(lastKnownLocation);
                loadPortalsFromOverpass(lastKnownLocation);
            } else {
                Log.e("GPS", "Localização não disponível para centralização");
                Toast.makeText(this, "Localização não disponível", Toast.LENGTH_SHORT).show();
            }
        });

        // Solicitar permissões
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET
        });

        Toast.makeText(this, "Carregando mapa...", Toast.LENGTH_LONG).show();
    }

    private String getRandomRank() {
        double rand = random.nextDouble() * 100;
        if (rand < 40) return "E"; // 40% chance
        else if (rand < 65) return "D"; // 25% chance
        else if (rand < 80) return "C"; // 15% chance
        else if (rand < 90) return "B"; // 10% chance
        else if (rand < 97) return "A"; // 7% chance
        else return "S"; // 3% chance
    }

    private void startLocationUpdates() {
        locationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (myLocationOverlay.getMyLocation() != null) {
                    GeoPoint currentLocation = myLocationOverlay.getMyLocation();
                    Log.d("GPS", "Localização atual: lat=" + currentLocation.getLatitude() + ", lon=" + currentLocation.getLongitude());
                    if (lastKnownLocation == null || calculateDistance(lastKnownLocation, currentLocation) > CACHE_LOCATION_THRESHOLD) {
                        lastKnownLocation = currentLocation;
                        loadPortalsFromOverpass(lastKnownLocation);
                    }
                } else {
                    Log.e("GPS", "Localização não disponível");
                }
                locationHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void loadPortalsFromOverpass(GeoPoint userLocation) {
        if (userLocation == null) {
            Log.e("Overpass", "Localização do usuário é nula");
            runOnUiThread(() -> Toast.makeText(Gps.this, "Localização não disponível", Toast.LENGTH_SHORT).show());
            return;
        }

        // Tenta carregar do cache
        if (loadPortalsFromCache(userLocation)) {
            Log.d("Cache", "Portais carregados do cache");
            runOnUiThread(() -> {
                mapView.getOverlays().clear();
                mapView.getOverlays().add(myLocationOverlay);
                addPortalsToMap();
                updatePortalScores(userLocation);
                Toast.makeText(Gps.this, "Carregados " + portalList.size() + " portais do cache", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // Carrega da API Overpass
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String lat = String.valueOf(userLocation.getLatitude());
                String lon = String.valueOf(userLocation.getLongitude());
                Log.d("Overpass", "Consultando portais em: lat=" + lat + ", lon=" + lon);
                String query = "[out:json];(node[amenity](around:5000," + lat + "," + lon + ");node[leisure](around:5000," + lat + "," + lon + ");node[tourism](around:5000," + lat + "," + lon + ");node[shop](around:5000," + lat + "," + lon + "););out body;";
                String url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");
                Log.d("Overpass", "URL da consulta: " + url);
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String jsonData = response.body().string();
                Log.d("Overpass", "Resposta da API (tamanho): " + jsonData.length() + " caracteres");
                JSONObject json = new JSONObject(jsonData);
                JSONArray elements = json.getJSONArray("elements");
                portalList.clear();
                int[] animationFrames = {
                        R.drawable.portalf01, R.drawable.portalf02, R.drawable.portalf03,
                        R.drawable.portalf04, R.drawable.portalf05, R.drawable.portalf06,
                        R.drawable.portalf07, R.drawable.portalf08, R.drawable.portalf09,
                        R.drawable.portalf10
                };
                String[] tipos = {"CAVERNA", "CAMPO", "CIDADE"};
                for (int i = 0; i < Math.min(elements.length(), 50); i++) {
                    JSONObject element = elements.getJSONObject(i);
                    double poiLat = element.getDouble("lat");
                    double poiLon = element.getDouble("lon");
                    String name = element.has("tags") && element.getJSONObject("tags").has("name") ?
                            element.getJSONObject("tags").getString("name") : "Portal Sem Nome";
                    DungeonType tipo = DungeonType.valueOf(tipos[random.nextInt(tipos.length)]);
                    String rank = getRandomRank();
                    long seed = (name + poiLat + poiLon).hashCode(); // Seed única
                    portalList.add(new Portal(new GeoPoint(poiLat, poiLon), "Portal para " + name, tipo, rank, 0, animationFrames, seed));
                    Log.d("Overpass", "Portal adicionado: " + name + " (" + poiLat + ", " + poiLon + ") - Tipo: " + tipo + ", Rank: " + rank + ", Seed: " + seed);
                }
                Log.d("Overpass", "Total de portais carregados: " + portalList.size());
                savePortalsToCache(userLocation);
                runOnUiThread(() -> {
                    mapView.getOverlays().clear();
                    mapView.getOverlays().add(myLocationOverlay);
                    addPortalsToMap();
                    updatePortalScores(userLocation);
                    Toast.makeText(Gps.this, "Carregados " + portalList.size() + " portais da API", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("Overpass", "Erro ao carregar portais: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(Gps.this, "Falha ao carregar portais: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private boolean loadPortalsFromCache(GeoPoint userLocation) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String portalJson = prefs.getString(KEY_PORTAL_CACHE, null);
        long cacheTimestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0);
        float cachedLat = prefs.getFloat(KEY_CACHE_LOCATION + "_lat", 0f);
        float cachedLon = prefs.getFloat(KEY_CACHE_LOCATION + "_lon", 0f);
        GeoPoint cachedLocation = new GeoPoint(cachedLat, cachedLon);

        if (portalJson != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_EXPIRY &&
                calculateDistance(userLocation, cachedLocation) < CACHE_LOCATION_THRESHOLD) {
            try {
                JSONArray jsonArray = new JSONArray(portalJson);
                portalList.clear();
                int[] animationFrames = {
                        R.drawable.portalf01, R.drawable.portalf02, R.drawable.portalf03,
                        R.drawable.portalf04, R.drawable.portalf05, R.drawable.portalf06,
                        R.drawable.portalf07, R.drawable.portalf08, R.drawable.portalf09,
                        R.drawable.portalf10
                };
                String[] validTipos = {"CAVERNA", "CAMPO", "CIDADE"};
                String[] validRanks = {"E", "D", "C", "B", "A", "S"};
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    double lat = json.getDouble("lat");
                    double lon = json.getDouble("lon");
                    String name = json.getString("name");
                    String type = json.getString("type");
                    String rank = json.getString("rank");
                    long seed = json.getLong("seed");
                    boolean validType = false;
                    for (String validTipo : validTipos) {
                        if (validTipo.equals(type)) {
                            validType = true;
                            break;
                        }
                    }
                    boolean validRank = false;
                    for (String validRankItem : validRanks) {
                        if (validRankItem.equals(rank)) {
                            validRank = true;
                            break;
                        }
                    }
                    if (!validType || !validRank) {
                        Log.d("Cache", "Cache contém dados inválidos (tipo: " + type + ", rank: " + rank + "). Ignorando cache.");
                        return false;
                    }
                    portalList.add(new Portal(new GeoPoint(lat, lon), name, DungeonType.valueOf(type), rank, 0, animationFrames, seed));
                }
                Log.d("Cache", "Carregados " + portalList.size() + " portais do cache");
                return true;
            } catch (Exception e) {
                Log.e("Cache", "Erro ao carregar portais do cache: " + e.getMessage());
            }
        }
        return false;
    }

    private void savePortalsToCache(GeoPoint userLocation) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            JSONArray jsonArray = new JSONArray();
            for (Portal portal : portalList) {
                JSONObject json = new JSONObject();
                json.put("lat", portal.getLocation().getLatitude());
                json.put("lon", portal.getLocation().getLongitude());
                json.put("name", portal.getName());
                json.put("type", portal.getType().toString());
                json.put("rank", portal.getRank());
                json.put("seed", portal.getSeed());
                jsonArray.put(json);
            }
            editor.putString(KEY_PORTAL_CACHE, jsonArray.toString());
            editor.putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis());
            editor.putFloat(KEY_CACHE_LOCATION + "_lat", (float) userLocation.getLatitude());
            editor.putFloat(KEY_CACHE_LOCATION + "_lon", (float) userLocation.getLongitude());
            editor.apply();
            Log.d("Cache", "Portais salvos no cache");
        } catch (Exception e) {
            Log.e("Cache", "Erro ao salvar portais no cache: " + e.getMessage());
        }
    }

    private void addPortalsToMap() {
        Log.d("Map", "Adicionando " + portalList.size() + " marcadores ao mapa");
        double zoomLevel = mapView.getZoomLevelDouble();
        Log.d("Map", "Nível de zoom atual: " + zoomLevel);
        for (Portal portal : portalList) {
            Marker marker = new Marker(mapView);
            marker.setPosition(portal.getLocation());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(portal.getName() + " (" + portal.getType() + ", Rank: " + portal.getRank() + ")");
            marker.setSnippet("Pontos: " + String.format("%.2f", portal.getPreferenceScore()));
            try {
                Drawable originalDrawable = getResources().getDrawable(portal.getAnimationFrames()[0], getTheme());
                int scaledSize = (int) (BASE_ICON_SIZE * Math.pow(2, zoomLevel - BASE_ZOOM_LEVEL));
                Bitmap originalBitmap = ((BitmapDrawable) originalDrawable).getBitmap();
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledSize, scaledSize, true);
                marker.setIcon(new BitmapDrawable(getResources(), scaledBitmap));
                Log.d("Map", "Ícone inicial definido para: " + portal.getName() + " (tamanho: " + scaledSize + "x" + scaledSize + ")");
            } catch (Exception e) {
                Log.e("Map", "Erro ao definir ícone para " + portal.getName() + ": " + e.getMessage());
            }
            portal.setMarker(marker);
            marker.setOnMarkerClickListener((m, mapView) -> {
                portal.setPreferenceScore(portal.getPreferenceScore() + 20);
                m.setSnippet("Pontos: " + String.format("%.2f", portal.getPreferenceScore()));
                m.setTitle(portal.getName() + " (" + portal.getType() + ", Rank: " + portal.getRank() + ")");
                m.showInfoWindow();
                Intent intent = new Intent(Gps.this, DungeonActivity.class);
                intent.putExtra(DungeonActivity.EXTRA_PORTAL_NAME, portal.getName());
                intent.putExtra(DungeonActivity.EXTRA_PORTAL_TYPE, portal.getType());
                intent.putExtra(DungeonActivity.EXTRA_PORTAL_RANK, portal.getRank());
                intent.putExtra(DungeonActivity.EXTRA_PORTAL_SEED, portal.getSeed());
                startActivity(intent);
                Toast.makeText(Gps.this, "Entrando no portal " + portal.getName(), Toast.LENGTH_SHORT).show();
                return true;
            });
            mapView.getOverlays().add(marker);
            startMarkerAnimation(portal);
        }
        mapView.invalidate();
        Log.d("Map", "Mapa invalidado para renderizar marcadores");
    }

    private void startMarkerAnimation(Portal portal) {
        final int[] frameIndex = {0};
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (portal.getMarker() != null && portal.getAnimationFrames().length > 0) {
                    double zoomLevel = mapView.getZoomLevelDouble();
                    frameIndex[0] = (frameIndex[0] + 1) % portal.getAnimationFrames().length;
                    try {
                        Drawable originalDrawable = getResources().getDrawable(portal.getAnimationFrames()[frameIndex[0]], getTheme());
                        int scaledSize = (int) (BASE_ICON_SIZE * Math.pow(2, zoomLevel - BASE_ZOOM_LEVEL));
                        Bitmap originalBitmap = ((BitmapDrawable) originalDrawable).getBitmap();
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledSize, scaledSize, true);
                        portal.getMarker().setIcon(new BitmapDrawable(getResources(), scaledBitmap));
                        Log.d("Animation", "Mudando para frame " + (frameIndex[0] + 1) + " em " + portal.getName() + " (tamanho: " + scaledSize + "x" + scaledSize + ")");
                        mapView.invalidate();
                    } catch (Exception e) {
                        Log.e("Animation", "Erro ao mudar frame para " + portal.getName() + ": " + e.getMessage());
                    }
                }
                animationHandler.postDelayed(this, ANIMATION_FRAME_INTERVAL);
            }
        }, ANIMATION_FRAME_INTERVAL);
    }

    private void updatePortalScores(GeoPoint userLocation) {
        if (userLocation == null) return;
        for (Portal portal : portalList) {
            double distance = calculateDistance(userLocation, portal.getLocation());
            double typeWeight = 1.0; // Peso uniforme
            double score = (100.0 / Math.max(distance, 1.0)) * typeWeight;
            portal.setPreferenceScore(score);
        }
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);
        addPortalsToMap();
    }

    private double calculateDistance(GeoPoint p1, GeoPoint p2) {
        double lat1 = Math.toRadians(p1.getLatitude());
        double lon1 = Math.toRadians(p1.getLongitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double lon2 = Math.toRadians(p2.getLongitude());
        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;
        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000 * c;
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                myLocationOverlay.enableMyLocation();
                myLocationOverlay.enableFollowLocation();
            } else {
                Toast.makeText(this, "Permissões de localização negadas", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
        }
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
            myLocationOverlay.disableFollowLocation();
        }
        locationHandler.removeCallbacksAndMessages(null);
        animationHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationHandler.removeCallbacksAndMessages(null);
        animationHandler.removeCallbacksAndMessages(null);
    }
}