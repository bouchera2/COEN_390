package com.coen390.team6;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GpsNavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "GpsNav";
    private static final int LOCATION_PERMISSION_CODE = 2001;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private Polyline currentRoute;

    // UI
    private EditText etSearch;
    private Button btnBack, btnStartRoute, btnExitRoute, btnRoutes;
    private Button btnZoomIn, btnZoomOut, btnMyLocation;
    private LinearLayout directionBanner, routeInfoPanel, timeLeftPanel;
    private View fatigueStatusCard;
    private View navLogItem;
    private View navSettingsItem;
    private TextView tvArrivalTime, tvArrivalAmPm, tvDistanceValue, tvRouteName;
    private TextView tvTimeLeft, tvTimeLeftUnit, tvTrafficStatus;
    private TextView tvDirectionText, tvDirectionDetail, tvDistance;
    private TextView tvFatigueEmoji, tvFatigueScore, tvGpsHeartRate;

    private boolean isNavigating = false;
    private final Handler sensorRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable sensorRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshDriverOverlay();
            sensorRefreshHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_navigation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        setupListeners();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void bindViews() {
        etSearch = findViewById(R.id.etSearchDestination);
        btnBack = findViewById(R.id.btnBack);
        btnStartRoute = findViewById(R.id.btnStartRoute);
        btnExitRoute = findViewById(R.id.btnExitRoute);
        btnRoutes = findViewById(R.id.btnRoutes);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        directionBanner = findViewById(R.id.directionBanner);
        routeInfoPanel = findViewById(R.id.routeInfoPanel);
        timeLeftPanel = findViewById(R.id.timeLeftPanel);
        fatigueStatusCard = findViewById(R.id.fatigueStatusCard);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
        tvArrivalTime = findViewById(R.id.tvArrivalTime);
        tvArrivalAmPm = findViewById(R.id.tvArrivalAmPm);
        tvDistanceValue = findViewById(R.id.tvDistanceValue);
        tvRouteName = findViewById(R.id.tvRouteName);
        tvTimeLeft = findViewById(R.id.tvTimeLeft);
        tvTimeLeftUnit = findViewById(R.id.tvTimeLeftUnit);
        tvTrafficStatus = findViewById(R.id.tvTrafficStatus);
        tvDirectionText = findViewById(R.id.tvDirectionText);
        tvDirectionDetail = findViewById(R.id.tvDirectionDetail);
        tvDistance = findViewById(R.id.tvDistance);
        tvFatigueEmoji = findViewById(R.id.tvFatigueEmoji);
        tvFatigueScore = findViewById(R.id.tvFatigueScore);
        tvGpsHeartRate = findViewById(R.id.tvGpsHeartRate);
    }

    private void setupListeners() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String destination = etSearch.getText().toString().trim();
                if (!destination.isEmpty()) {
                    searchAndRoute(destination);
                }
                return true;
            }
            return false;
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnStartRoute.setOnClickListener(v -> {
            String destination = etSearch.getText().toString().trim();
            if (!destination.isEmpty()) {
                searchAndRoute(destination);
            } else {
                Toast.makeText(this, "Enter a destination first", Toast.LENGTH_SHORT).show();
            }
        });

        btnExitRoute.setOnClickListener(v -> exitNavigation());

        btnZoomIn.setOnClickListener(v -> {
            if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomIn());
        });

        btnZoomOut.setOnClickListener(v -> {
            if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomOut());
        });

        btnMyLocation.setOnClickListener(v -> centerOnMyLocation());

        fatigueStatusCard.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));

        navLogItem.setOnClickListener(v ->
                startActivity(new Intent(this, DriverLogActivity.class)));

        navSettingsItem.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false); // We have custom buttons
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Dark map style for the app theme
        try {
            mMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style_dark));
        } catch (Exception e) {
            Log.w(TAG, "Map style not found, using default");
        }

        if (checkLocationPermission()) {
            enableMyLocation();
        }
    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);

        // Get last known location to center the map immediately
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos, 15f));
            }
        });

        // Start continuous location updates
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    currentLocation = loc;
                    // Update Firestore with live location (for fleet manager)
                    updateLocationInFirestore(loc);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateLocationInFirestore(Location loc) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) return;

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("lastKnownLat", loc.getLatitude());
        updates.put("lastKnownLng", loc.getLongitude());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(uid)
                .update(updates)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update location", e));
    }

    private void centerOnMyLocation() {
        if (currentLocation != null && mMap != null) {
            LatLng myPos = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos, 16f));
        }
    }

    private void refreshDriverOverlay() {
        DriverFatigueStatus fatigueStatus = DriverFatigueStatus.from(this);
        tvFatigueEmoji.setText(fatigueStatus.getEmoji());
        tvFatigueScore.setText(fatigueStatus.getScoreText());
        tvFatigueScore.setTextColor(fatigueStatus.getAccentColor());

        boolean fingerDetected = BleSensorPreferences.isFingerDetected(this);
        int avgBpm = BleSensorPreferences.getAvgBpm(this);
        float bpm = BleSensorPreferences.getBpm(this);
        if (!fingerDetected) {
            tvGpsHeartRate.setText("--");
        } else {
            int displayBpm = avgBpm > 0 ? avgBpm : Math.round(bpm);
            tvGpsHeartRate.setText(displayBpm > 0 ? String.valueOf(displayBpm) : "--");
        }
    }

    private void searchAndRoute(String destination) {
        if (currentLocation == null) {
            Toast.makeText(this, "Waiting for GPS location...", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Calculating route...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String apiKey = getApiKey();
                String origin = currentLocation.getLatitude() + "," + currentLocation.getLongitude();
                String encodedDest = URLEncoder.encode(destination, "UTF-8");

                String urlStr = "https://maps.googleapis.com/maps/api/directions/json"
                        + "?origin=" + origin
                        + "&destination=" + encodedDest
                        + "&mode=driving"
                        + "&key=" + apiKey;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                String status = json.getString("status");

                if (!"OK".equals(status)) {
                    runOnUiThread(() -> Toast.makeText(this, "Route not found: " + status, Toast.LENGTH_LONG).show());
                    return;
                }

                JSONObject route = json.getJSONArray("routes").getJSONObject(0);
                JSONObject leg = route.getJSONArray("legs").getJSONObject(0);

                // Extract route polyline
                String encodedPolyline = route.getJSONObject("overview_polyline").getString("points");
                List<LatLng> routePoints = decodePolyline(encodedPolyline);

                // Extract info
                String distanceText = leg.getJSONObject("distance").getString("text");
                int durationSeconds = leg.getJSONObject("duration").getInt("value");
                String endAddress = leg.getString("end_address");
                LatLng destLatLng = new LatLng(
                        leg.getJSONObject("end_location").getDouble("lat"),
                        leg.getJSONObject("end_location").getDouble("lng"));

                // Get first step for direction banner
                JSONObject firstStep = leg.getJSONArray("steps").getJSONObject(0);
                String firstInstruction = firstStep.getString("html_instructions")
                        .replaceAll("<[^>]*>", ""); // Strip HTML tags
                String stepDistance = firstStep.getJSONObject("distance").getString("text");

                // Calculate arrival time
                long arrivalMillis = System.currentTimeMillis() + (durationSeconds * 1000L);
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm", Locale.getDefault());
                SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());
                String arrivalTime = timeFormat.format(new Date(arrivalMillis));
                String arrivalAmPm = amPmFormat.format(new Date(arrivalMillis));

                // Duration text
                int hours = durationSeconds / 3600;
                int minutes = (durationSeconds % 3600) / 60;
                String timeLeftText = hours > 0 ? hours + "h " + minutes + "m" : minutes + "";
                String timeLeftUnitText = hours > 0 ? "" : " min";

                runOnUiThread(() -> {
                    // Draw route on map
                    if (currentRoute != null) currentRoute.remove();
                    mMap.clear();
                    currentRoute = mMap.addPolyline(new PolylineOptions()
                            .addAll(routePoints)
                            .width(10)
                            .color(Color.parseColor("#2563EB"))
                            .geodesic(true));

                    // Add destination marker
                    mMap.addMarker(new MarkerOptions()
                            .position(destLatLng)
                            .title(endAddress));

                    // Zoom to show full route
                    com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder =
                            new com.google.android.gms.maps.model.LatLngBounds.Builder();
                    for (LatLng point : routePoints) boundsBuilder.include(point);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));

                    // Update UI panels
                    tvArrivalTime.setText(arrivalTime);
                    tvArrivalAmPm.setText(arrivalAmPm);
                    tvDistanceValue.setText(distanceText.replace(" km", "").replace(" mi", ""));
                    tvRouteName.setText(endAddress.length() > 30 ? endAddress.substring(0, 30) + "..." : endAddress);
                    tvTimeLeft.setText(timeLeftText);
                    tvTimeLeftUnit.setText(timeLeftUnitText);
                    tvTrafficStatus.setText("Clear Traffic");
                    tvDirectionText.setText(firstInstruction);
                    tvDirectionDetail.setText(endAddress);
                    tvDistance.setText(stepDistance);

                    // Show navigation UI
                    directionBanner.setVisibility(View.VISIBLE);
                    routeInfoPanel.setVisibility(View.VISIBLE);
                    timeLeftPanel.setVisibility(View.VISIBLE);
                    btnStartRoute.setVisibility(View.GONE);
                    btnExitRoute.setVisibility(View.VISIBLE);
                    btnRoutes.setVisibility(View.VISIBLE);
                    etSearch.setVisibility(View.GONE);

                    isNavigating = true;
                });

            } catch (Exception e) {
                Log.e(TAG, "Direction API error", e);
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void exitNavigation() {
        isNavigating = false;
        if (currentRoute != null) currentRoute.remove();
        mMap.clear();

        directionBanner.setVisibility(View.GONE);
        routeInfoPanel.setVisibility(View.GONE);
        timeLeftPanel.setVisibility(View.GONE);
        btnStartRoute.setVisibility(View.VISIBLE);
        btnExitRoute.setVisibility(View.GONE);
        btnRoutes.setVisibility(View.GONE);
        etSearch.setVisibility(View.VISIBLE);
        etSearch.setText("");

        centerOnMyLocation();
    }

    private String getApiKey() {
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager()
                    .getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) {
            return "";
        }
    }

    // Decode Google's encoded polyline format
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            poly.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
        }
        return poly;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        sensorRefreshHandler.removeCallbacks(sensorRefreshRunnable);
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDriverOverlay();
        sensorRefreshHandler.post(sensorRefreshRunnable);
        if (checkLocationPermission() && mMap != null) {
            enableMyLocation();
        }
    }
}
