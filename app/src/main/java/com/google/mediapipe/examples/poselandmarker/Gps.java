package com.google.mediapipe.examples.poselandmarker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.util.ArrayList;
import java.util.List;

public class Gps extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;

    // Chave e ID do estilo MapTiler (atualizado com a nova chave e estilo)
    private static final String MAPTILER_KEY = "OQLaynlp6gA08keMzOwZ";
    private static final String MAPTILER_STYLE = "streets-v2-dark";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Carrega configurações do OSMDroid
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_gps);

        // Configura o MapView
        mapView = findViewById(R.id.mapFragment);
        mapView.setMultiTouchControls(true);

        // TileSource personalizado do MapTiler (usando o novo estilo e chave)
        OnlineTileSourceBase mapTilerSource = new OnlineTileSourceBase(
                "MapTilerRaster",
                0, 22, 256, ".png",
                new String[]{"https://api.maptiler.com/maps/" + MAPTILER_STYLE + "/"}
        ) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                String url = getBaseUrl()
                        + MapTileIndex.getZoom(pMapTileIndex) + "/"
                        + MapTileIndex.getX(pMapTileIndex) + "/"
                        + MapTileIndex.getY(pMapTileIndex)
                        + ".png?key=" + MAPTILER_KEY;
                Log.d("MapTiler", "Tile URL: " + url); // Log para depuração
                return url;
            }
        };

        // TileSource de fallback (OpenStreetMap, caso haja falha)
        OnlineTileSourceBase fallbackSource = new XYTileSource(
                "OSMFallback",
                0, 20, 256, ".png",
                new String[]{"https://tile.openstreetmap.org/"}
        );

        // Define o tile source (tenta MapTiler primeiro)
        try {
            mapView.setTileSource(mapTilerSource);
        } catch (Exception e) {
            Log.e("MapTiler", "Falha ao carregar MapTiler, usando fallback", e);
            mapView.setTileSource(fallbackSource);
            Toast.makeText(this, "Erro ao carregar tiles do MapTiler, usando mapa padrão", Toast.LENGTH_LONG).show();
        }

        // Configura controlador do mapa
        IMapController mapController = mapView.getController();
        mapController.setZoom(18.0);
        mapController.setCenter(new GeoPoint(0.0, 0.0)); // Inicial, será ajustado pela localização

        // Ativa localização
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (myLocationOverlay.getMyLocation() != null) {
                mapController.animateTo(myLocationOverlay.getMyLocation());
                Toast.makeText(Gps.this, "Localização encontrada", Toast.LENGTH_SHORT).show();
            }
        }));
        mapView.getOverlays().add(myLocationOverlay);

        // Configura o botão de centralização
        Button btnCenterLocation = findViewById(R.id.btnCenterLocation);
        btnCenterLocation.setOnClickListener(v -> {
            if (myLocationOverlay.getMyLocation() != null) {
                mapController.animateTo(myLocationOverlay.getMyLocation());
            } else {
                Toast.makeText(this, "Localização não disponível", Toast.LENGTH_SHORT).show();
            }
        });

        // Solicita permissões
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        // Feedback de inicialização
        Toast.makeText(this, "Carregando mapa...", Toast.LENGTH_LONG).show();
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
            myLocationOverlay.disableFollowLocation();
        }
    }
}