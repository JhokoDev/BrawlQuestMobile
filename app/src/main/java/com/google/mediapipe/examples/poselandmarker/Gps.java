package com.google.mediapipe.examples.poselandmarker;

import android.Manifest;
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
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class Gps extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private static final String MAPTILER_KEY = "OQLaynlp6gA08keMzOwZ";
    private static final String MAPTILER_STYLE = "streets-v2-dark";
    private List<PointOfInterest> poiList = new ArrayList<>();
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private final Handler animationHandler = new Handler(Looper.getMainLooper());
    private GeoPoint lastKnownLocation;
    private static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 segundos
    private static final long ANIMATION_FRAME_INTERVAL = 200; // 200ms por frame (150% mais rápido)
    private static final int BASE_ICON_SIZE = 200; // Tamanho padrão do ícone em pixels no zoom base
    private static final double BASE_ZOOM_LEVEL = 18.0; // Zoom base para o tamanho padrão

    public static class PointOfInterest {
        private GeoPoint location;
        private String name;
        private String type;
        private double preferenceScore;
        private int[] animationFrames;
        private Marker marker;

        public PointOfInterest(GeoPoint location, String name, String type, double preferenceScore, int[] animationFrames) {
            this.location = location;
            this.name = name;
            this.type = type;
            this.preferenceScore = preferenceScore;
            this.animationFrames = animationFrames;
        }

        public GeoPoint getLocation() { return location; }
        public String getName() { return name; }
        public String getType() { return type; }
        public double getPreferenceScore() { return preferenceScore; }
        public void setPreferenceScore(double score) { this.preferenceScore = score; }
        public int[] getAnimationFrames() { return animationFrames; }
        public void setMarker(Marker marker) { this.marker = marker; }
        public Marker getMarker() { return marker; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_gps);

        mapView = findViewById(R.id.mapFragment);
        mapView.setMultiTouchControls(true);

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

        IMapController mapController = mapView.getController();
        mapController.setZoom(18.0);
        mapController.setCenter(new GeoPoint(0.0, 0.0));

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (myLocationOverlay.getMyLocation() != null) {
                lastKnownLocation = myLocationOverlay.getMyLocation();
                Log.d("GPS", "Localização inicial: lat=" + lastKnownLocation.getLatitude() + ", lon=" + lastKnownLocation.getLongitude());
                mapController.animateTo(lastKnownLocation);
                loadPoisFromOverpass(lastKnownLocation);
                Toast.makeText(Gps.this, "Localização encontrada", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("GPS", "Localização inicial não disponível");
                Toast.makeText(Gps.this, "Localização não encontrada", Toast.LENGTH_SHORT).show();
            }
        }));
        mapView.getOverlays().add(myLocationOverlay);

        startLocationUpdates();

        Button btnCenterLocation = findViewById(R.id.btnCenterLocation);
        btnCenterLocation.setOnClickListener(v -> {
            if (myLocationOverlay.getMyLocation() != null) {
                lastKnownLocation = myLocationOverlay.getMyLocation();
                Log.d("GPS", "Centralizando em: lat=" + lastKnownLocation.getLatitude() + ", lon=" + lastKnownLocation.getLongitude());
                mapController.animateTo(lastKnownLocation);
                loadPoisFromOverpass(lastKnownLocation);
            } else {
                Log.e("GPS", "Localização não disponível para centralização");
                Toast.makeText(this, "Localização não disponível", Toast.LENGTH_SHORT).show();
            }
        });

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET
        });

        Toast.makeText(this, "Carregando mapa...", Toast.LENGTH_LONG).show();
    }

    private void startLocationUpdates() {
        locationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (myLocationOverlay.getMyLocation() != null) {
                    GeoPoint currentLocation = myLocationOverlay.getMyLocation();
                    Log.d("GPS", "Localização atual: lat=" + currentLocation.getLatitude() + ", lon=" + currentLocation.getLongitude());
                    if (lastKnownLocation == null || calculateDistance(lastKnownLocation, currentLocation) > 50) {
                        lastKnownLocation = currentLocation;
                        loadPoisFromOverpass(lastKnownLocation);
                    }
                } else {
                    Log.e("GPS", "Localização não disponível");
                }
                locationHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void loadPoisFromOverpass(GeoPoint userLocation) {
        if (userLocation == null) {
            Log.e("Overpass", "Localização do usuário é nula");
            runOnUiThread(() -> Toast.makeText(Gps.this, "Localização não disponível", Toast.LENGTH_SHORT).show());
            return;
        }
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String lat = String.valueOf(userLocation.getLatitude());
                String lon = String.valueOf(userLocation.getLongitude());
                Log.d("Overpass", "Consultando POIs em: lat=" + lat + ", lon=" + lon);
                String query = "[out:json];(node[amenity](around:5000," + lat + "," + lon + ");node[leisure](around:5000," + lat + "," + lon + ");node[tourism](around:5000," + lat + "," + lon + ");node[shop](around:5000," + lat + "," + lon + "););out body;";
                String url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");
                Log.d("Overpass", "URL da consulta: " + url);
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String jsonData = response.body().string();
                Log.d("Overpass", "Resposta da API (tamanho): " + jsonData.length() + " caracteres");
                Log.d("Overpass", "Resposta da API: " + jsonData);
                JSONObject json = new JSONObject(jsonData);
                JSONArray elements = json.getJSONArray("elements");
                poiList.clear();
                int[] animationFrames = {
                        R.drawable.portalf01, R.drawable.portalf02, R.drawable.portalf03,
                        R.drawable.portalf04, R.drawable.portalf05, R.drawable.portalf06,
                        R.drawable.portalf07, R.drawable.portalf08, R.drawable.portalf09,
                        R.drawable.portalf10
                };
                for (int i = 0; i < Math.min(elements.length(), 50); i++) {
                    JSONObject element = elements.getJSONObject(i);
                    double poiLat = element.getDouble("lat");
                    double poiLon = element.getDouble("lon");
                    String name = element.has("tags") && element.getJSONObject("tags").has("name") ?
                            element.getJSONObject("tags").getString("name") : "Portal Sem Nome";
                    String type = element.getJSONObject("tags").has("amenity") ? "dungeon_geral" :
                            element.getJSONObject("tags").has("leisure") ? "dungeon_lazer" :
                                    element.getJSONObject("tags").has("tourism") ? "dungeon_turismo" : "dungeon_comercial";
                    poiList.add(new PointOfInterest(new GeoPoint(poiLat, poiLon), "Portal para " + name, type, 0, animationFrames));
                    Log.d("Overpass", "POI adicionado: " + name + " (" + poiLat + ", " + poiLon + ")");
                }
                Log.d("Overpass", "Total de POIs carregados: " + poiList.size());
                runOnUiThread(() -> {
                    mapView.getOverlays().clear();
                    mapView.getOverlays().add(myLocationOverlay);
                    addPoisToMap();
                    updatePoiScores(userLocation);
                    Toast.makeText(Gps.this, "Carregados " + poiList.size() + " portais", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("Overpass", "Erro ao carregar POIs: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(Gps.this, "Falha ao carregar portais: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void addPoisToMap() {
        Log.d("Map", "Adicionando " + poiList.size() + " marcadores ao mapa");
        double zoomLevel = mapView.getZoomLevelDouble();
        Log.d("Map", "Nível de zoom atual: " + zoomLevel);
        for (PointOfInterest poi : poiList) {
            Marker marker = new Marker(mapView);
            marker.setPosition(poi.getLocation());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(poi.getName() + " (" + poi.getType() + ")");
            marker.setSnippet("Pontos: " + String.format("%.2f", poi.getPreferenceScore()));
            try {
                // Escala o ícone com base no zoom
                Drawable originalDrawable = getResources().getDrawable(poi.getAnimationFrames()[0], getTheme());
                int scaledSize = (int) (BASE_ICON_SIZE * Math.pow(2, zoomLevel - BASE_ZOOM_LEVEL));
                Bitmap originalBitmap = ((BitmapDrawable) originalDrawable).getBitmap();
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledSize, scaledSize, true);
                marker.setIcon(new BitmapDrawable(getResources(), scaledBitmap));
                Log.d("Map", "Ícone inicial definido para: " + poi.getName() + " (tamanho: " + scaledSize + "x" + scaledSize + ")");
            } catch (Exception e) {
                Log.e("Map", "Erro ao definir ícone para " + poi.getName() + ": " + e.getMessage());
            }
            poi.setMarker(marker);
            marker.setOnMarkerClickListener((m, mapView) -> {
                poi.setPreferenceScore(poi.getPreferenceScore() + 20);
                m.setSnippet("Pontos: " + String.format("%.2f", poi.getPreferenceScore()));
                m.showInfoWindow();
                Toast.makeText(Gps.this, "Entrando no portal " + poi.getName(), Toast.LENGTH_SHORT).show();
                return true;
            });
            mapView.getOverlays().add(marker);
            startMarkerAnimation(poi);
        }
        mapView.invalidate();
        Log.d("Map", "Mapa invalidado para renderizar marcadores");
    }

    private void startMarkerAnimation(PointOfInterest poi) {
        final int[] frameIndex = {0};
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (poi.getMarker() != null && poi.getAnimationFrames().length > 0) {
                    double zoomLevel = mapView.getZoomLevelDouble();
                    frameIndex[0] = (frameIndex[0] + 1) % poi.getAnimationFrames().length;
                    try {
                        // Escala o frame com base no zoom atual
                        Drawable originalDrawable = getResources().getDrawable(poi.getAnimationFrames()[frameIndex[0]], getTheme());
                        int scaledSize = (int) (BASE_ICON_SIZE * Math.pow(2, zoomLevel - BASE_ZOOM_LEVEL));
                        Bitmap originalBitmap = ((BitmapDrawable) originalDrawable).getBitmap();
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledSize, scaledSize, true);
                        poi.getMarker().setIcon(new BitmapDrawable(getResources(), scaledBitmap));
                        Log.d("Animation", "Mudando para frame " + (frameIndex[0] + 1) + " em " + poi.getName() + " (tamanho: " + scaledSize + "x" + scaledSize + ")");
                        mapView.invalidate();
                    } catch (Exception e) {
                        Log.e("Animation", "Erro ao mudar frame para " + poi.getName() + ": " + e.getMessage());
                    }
                }
                animationHandler.postDelayed(this, ANIMATION_FRAME_INTERVAL);
            }
        }, ANIMATION_FRAME_INTERVAL);
    }

    private void updatePoiScores(GeoPoint userLocation) {
        if (userLocation == null) return;
        for (PointOfInterest poi : poiList) {
            double distance = calculateDistance(userLocation, poi.getLocation());
            double typeWeight = 1.0; // Peso uniforme para POIs generalizados
            double score = (100.0 / Math.max(distance, 1.0)) * typeWeight;
            poi.setPreferenceScore(score);
        }
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);
        addPoisToMap();
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