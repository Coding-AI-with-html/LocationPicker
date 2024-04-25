package com.example.myapplication;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityLocationPickerBinding;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocationPicker extends AppCompatActivity implements OnMapReadyCallback {



    private ActivityLocationPickerBinding binding;

    private static final String TAG = "LOCATION_PICKER_TAG";

    private static final int DEFAULT_ZOOM = 15;
    private GoogleMap mMap = null;

    private PlacesClient mPlaceClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;



    private Location mLastKnowLocation = null;
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private String selectedAdress = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //init view binding activity_location_picker.xml = ActivityLocationPickerBinding
        binding = ActivityLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //hide the doneLl for now. It will show when user select or search location
        binding.doneLl.setVisibility(View.GONE);

        //Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        mapFragment.getMapAsync(this);


        // Initialize The places client
        Places.initialize(this, getString(R.string.google_api_key));


        mPlaceClient = Places.createClient(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment)getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);


        Place.Field[] placesList = new Place.Field[]{Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG};
        //set Location fields to the autoComllSuportFragment
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(placesList));
        //listen for place selections
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                //exception occured while searching/picking location
                Log.d(TAG, "onError: Status:  " + status);
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                //Place selected. The param place contain all fields that we set as list. Now return the requested location fields back to the requesting acitivirty/Fragment

                String id = place.getId();
                String title = place.getName();
                LatLng latLng = place.getLatLng();
                selectedLatitude = latLng.latitude;
                selectedLongitude = latLng.longitude;


                selectedAdress = place.getAddress();

                Log.d(TAG, "onPlaceSelected: ID: " + id);
                Log.d(TAG, "onPlaceSelected: Title : " + title);
                Log.d(TAG, "onPlaceSelected: Latitude: "+ selectedLatitude);
                Log.d(TAG, "onPlaceSelected: Longitude: "+ selectedLongitude);



                addMarker(latLng, title, selectedAdress);
            }
        });


        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onBackPressed();
            }
        });

        binding.toolbarGpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isGPSEnabled()){
                    requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                } else {

                    Toast.makeText(LocationPicker.this, "Location is not turned on! Turn it on to show current location!",Toast.LENGTH_LONG).show();
                }
            }
        });

        binding.doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.putExtra("latitude", selectedLatitude);
                intent.putExtra("longitude", selectedLongitude);
                intent.putExtra("address: ", selectedAdress);
                setResult(RESULT_OK,intent);
                finish();
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady: ");

        mMap = googleMap;

        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {


                selectedLatitude = latLng.latitude;
                selectedLongitude = latLng.longitude;


                Log.d(TAG, "onMapClick: selectedLatitude:  " + selectedLatitude);
                Log.d(TAG, "onMapClick: " + selectedLongitude);

                addressFromLatLng(latLng);
            }
        });

    }

    @SuppressLint("MissingPermission")
    private ActivityResultLauncher<String> requestLocationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {

                @Override
                public void onActivityResult(Boolean result) {
                    Log.d(TAG, "onActivityResult: ");

                    if(result){
                        mMap.setMyLocationEnabled(true);
                        pickCurrentPlace();
                    } else {

                        Toast.makeText(LocationPicker.this, "Permission denied....!",Toast.LENGTH_LONG).show();

                    }
                }
            }
    );


    private void addressFromLatLng(LatLng latLng){
        Log.d(TAG, "addressFromLatLng: ");

        Geocoder geocoder = new Geocoder(this);

        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            Address address = addressList.get(0);

            String addresLine = address.getAddressLine(0);
            String CountryName = address.getCountryName();
            String adminArea = address.getAdminArea();
            String subAdminArea = address.getSubAdminArea();
            String locality = address.getLocality();
            String subLocality = address.getSubLocality();
            String postalCode = address.getPostalCode();

            selectedAdress = ""+ addresLine;

            addMarker(latLng, ""+ subLocality, ""+addresLine);
        }catch (Exception e){
            Log.d(TAG, "addressFromLatLng: ",e);
        }

    }



    /**
     * this function will be called only if location permission is granted
     * We will only check if map object is not null then proceed to show location on map
     */
    private void pickCurrentPlace(){
        Log.d(TAG, "pickCurrentPlace: ");

        if(mMap == null){
            return;
        }

        detectANdShowDeviceLocationMap();
    }

    /**
     * get the cureent location of the device, and position the maps camera
     * @return
     */
    @SuppressLint("MissingPermission")
    private void detectANdShowDeviceLocationMap(){


        try {

            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    if(location != null){


                        mLastKnowLocation = location;

                        selectedLatitude = location.getLatitude();
                        selectedLongitude = location.getLongitude();

                        Log.d(TAG, "onSuccess: selectedLatitude: " + selectedLatitude);
                        Log.d(TAG, "onSuccess: selectedLongitude: " + selectedLongitude);

                        LatLng  latLng = new LatLng(selectedLatitude, selectedLongitude);

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));

                        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

                        addressFromLatLng(latLng);

                    } else {
                        Log.d(TAG, "onSuccess: LOCATION IS NULL");
                    }


                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "onFailure: ", e);
                        }
                    });
        }catch (Exception e){
            Log.d(TAG, "detectANdShowDeviceLocationMap: ", e);
        }
    }


    /**
    Check if gps is enabled or not
     */
    private boolean isGPSEnabled(){

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean gpsEnabled = false;

        boolean networkEnabled = false;

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        }catch (Exception e){
            Log.d(TAG, "isGPSEnabled: ", e);
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        } catch (Exception e){
            Log.d(TAG, "isGPSEnabled: ", e);
        }

        //return results
        return !(!gpsEnabled && !networkEnabled);
    }


    /**

     * @param latLng location picked
     * @param title Title of location picked
     * @param selectedAdress Addres of the location picked
     */

    private void addMarker(LatLng latLng, String title, String selectedAdress){


        Log.d(TAG, "addMarker: latitude: " + latLng.latitude);
        Log.d(TAG, "addMarker: longitude:  " + latLng.longitude);
        Log.d(TAG, "addMarker: Title:  " + title);
        Log.d(TAG, "addMarker: address: " + selectedAdress);

        mMap.clear();

        try {

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(""+title);
            markerOptions.snippet(""+selectedAdress);

            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

            mMap.addMarker(markerOptions);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));

            // show the doneLl, so user can gobakc with selected location to the acitivity/fragment class that is requesting the location
            binding.doneLl.setVisibility(View.VISIBLE); //set selected lcoation compllete adress
            binding.selectedPlaceTv.setText(selectedAdress);
        } catch (Exception e){
            Log.d(TAG, "addMarker: ",  e);
        }

    }
}