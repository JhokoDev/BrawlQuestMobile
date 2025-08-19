package com.google.mediapipe.examples.poselandmarker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
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
    private GeoPoint lastKnownLocation;
    private static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 segundos

    // Classe para representar um Portal para Dungeon
    public static class PointOfInterest {
        private GeoPoint location;
        private String name;
        private String type; // Ex.: "dungeon_natureza", "dungeon_histórico"
        private double preferenceScore;

        public PointOfInterest(GeoPoint location, String name, String type, double preferenceScore) {
            this.location = location;
            this.name = name;
            this.type = type;
            this.preferenceScore = preferenceScore;
        }

        public GeoPoint getLocation() { return location; }
        public String getName() { return name; }
        public String getType() { return type; }
        public double getPreferenceScore() { return preferenceScore; }
        public void setPreferenceScore(double score) { this.preferenceScore = score; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_gps);

        mapView = findViewById(R.id.mapFragment);
        mapView.setMultiTouchControls(true);

        // TileSource do MapTiler
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

        IMapController mapController = mapView.getController();
        mapController.setZoom(18.0);
        mapController.setCenter(new GeoPoint(0.0, 0.0));

        // Ativa localização
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (myLocationOverlay.getMyLocation() != null) {
                lastKnownLocation = myLocationOverlay.getMyLocation();
                mapController.animateTo(lastKnownLocation);
                loadPoisFromOverpass(lastKnownLocation);
                Toast.makeText(Gps.this, "Localização encontrada", Toast.LENGTH_SHORT).show();
            }
        }));
        mapView.getOverlays().add(myLocationOverlay);

        // Monitora mudanças de localização
        startLocationUpdates();

        // Botão de centralização
        Button btnCenterLocation = findViewById(R.id.btnCenterLocation);
        btnCenterLocation.setOnClickListener(v -> {
            if (myLocationOverlay.getMyLocation() != null) {
                lastKnownLocation = myLocationOverlay.getMyLocation();
                mapController.animateTo(lastKnownLocation);
                loadPoisFromOverpass(lastKnownLocation);
            } else {
                Toast.makeText(this, "Localização não disponível", Toast.LENGTH_SHORT).show();
            }
        });

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        Toast.makeText(this, "Carregando mapa...", Toast.LENGTH_LONG).show();
    }

    // Monitora mudanças de localização periodicamente
    private void startLocationUpdates() {
        locationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (myLocationOverlay.getMyLocation() != null) {
                    GeoPoint currentLocation = myLocationOverlay.getMyLocation();
                    if (lastKnownLocation == null || calculateDistance(lastKnownLocation, currentLocation) > 50) { // Atualiza se mover > 50m
                        lastKnownLocation = currentLocation;
                        loadPoisFromOverpass(lastKnownLocation);
                    }
                }
                locationHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    // Carrega POIs dinâmicos da Overpass API
    private void loadPoisFromOverpass(GeoPoint userLocation) {
        if (userLocation == null) return;
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String lat = String.valueOf(userLocation.getLatitude());
                String lon = String.valueOf(userLocation.getLongitude());
                String query = "[out:json];(node[\"amenity\"~\"park|monument\"](around:5000," + lat + "," + lon + "););out body;";
                String url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String jsonData = response.body().string();
                JSONObject json = new JSONObject(jsonData);
                JSONArray elements = json.getJSONArray("elements");
                poiList.clear();
                for (int i = 0; i < elements.length(); i++) {
                    JSONObject element = elements.getJSONObject(i);
                    double poiLat = element.getDouble("lat");
                    double poiLon = element.getDouble("lon");
                    String name = element.has("tags") && element.getJSONObject("tags").has("name") ?
                            element.getJSONObject("tags").getString("name") : "Portal Sem Nome";
                    String type = element.getJSONObject("tags").has("amenity") ? "dungeon_natureza" : "dungeon_histórico";
                    poiList.add(new PointOfInterest(new GeoPoint(poiLat, poiLon), "Portal para " + name, type, 0));
                }
                runOnUiThread(() -> {
                    mapView.getOverlays().clear();
                    mapView.getOverlays().add(myLocationOverlay);
                    addPoisToMap();
                    updatePoiScores(userLocation);
                });
            } catch (Exception e) {
                Log.e("Overpass", "Erro ao carregar POIs: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(Gps.this, "Falha ao carregar portais", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Adiciona marcadores de portais ao mapa
    private void addPoisToMap() {
        for (PointOfInterest poi : poiList) {
            Marker marker = new Marker(mapView);
            marker.setPosition(poi.getLocation());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(poi.getName() + " (" + poi.getType() + ")");
            marker.setSnippet("Pontos: " + String.format("%.2f", poi.getPreferenceScore()));
            marker.setOnMarkerClickListener((m, mapView) -> {
                poi.setPreferenceScore(poi.getPreferenceScore() + 20);
                m.setSnippet("Pontos: " + String.format("%.2f", poi.getPreferenceScore()));
                m.showInfoWindow();
                Toast.makeText(Gps.this, "Entrando no portal " + poi.getName(), Toast.LENGTH_SHORT).show();
                return true;
            });
            mapView.getOverlays().add(marker);
        }
        mapView.invalidate();
    }

    // Calcula pontos de preferência
    private void updatePoiScores(GeoPoint userLocation) {
        if (userLocation == null) return;
        for (PointOfInterest poi : poiList) {
            double distance = calculateDistance(userLocation, poi.getLocation());
            double typeWeight;
            switch (poi.getType()) {
                case "dungeon_histórico":
                    typeWeight = 1.5;
                    break;
                case "dungeon_natureza":
                    typeWeight = 1.2;
                    break;
                default:
                    typeWeight = 1.0;
                    break;
            }
            double score = (100.0 / Math.max(distance, 1.0)) * typeWeight;
            poi.setPreferenceScore(score);
        }
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);
        addPoisToMap();
    }

    // Calcula distância em metros usando Haversine
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationHandler.removeCallbacksAndMessages(null);
    }
}