package com.example.stirling_charlie_s2530655;

import androidx.fragment.app.FragmentActivity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.stirling_charlie_s2530655.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backButton.setOnClickListener(v -> finish());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng GCU = new LatLng(55.8668, -4.2500);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(GCU, 8f));

        for (TrafficEvent event : EventStore.events) { // get situations
            // skip events that never got coordinates
            if (event.getFromLat() == 0.0 && event.getFromLon() == 0.0) continue;

            LatLng position = new LatLng(event.getFromLat(), event.getFromLon());
            mMap.addMarker(new MarkerOptions() // create markers from situations
                    .position(position)
                    .title(event.getImpactType())
                    .snippet(event.getGeneralPublicComment())
                    .icon(BitmapDescriptorFactory.defaultMarker(hueFor(event))));

            mMap.addPolyline(new PolylineOptions() // create polylines from situations
                    .add(new LatLng(event.getFromLat(), event.getFromLon()),
                            new LatLng(event.getToLat(), event.getToLon()))
                    .color(Color.RED)
                    .width(8f));
            // polylines don't follow road geometry, implementation is possible but each request gets billed

            // debug for checking colours work, better to comment out roadworks download in mainactivity
            // Log.d("Map", "impact=" + event.getImpactType() + " hue=" + hueFor(event)); // troubleshoot marker colour
        }
    }

    private float hueFor(TrafficEvent event) { // set colour of markers depending on impact type
        switch (event.getImpactType()) {
            case "Road Closure":    return BitmapDescriptorFactory.HUE_RED;
            case "Road Works":      return BitmapDescriptorFactory.HUE_YELLOW;
            case "Queuing Traffic": return BitmapDescriptorFactory.HUE_BLUE;
            default:                return BitmapDescriptorFactory.HUE_MAGENTA;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }



}