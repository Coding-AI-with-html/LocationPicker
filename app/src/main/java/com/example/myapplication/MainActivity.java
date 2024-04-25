package com.example.myapplication;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    private double latitude = 0;
    private double longitude = 0;

    private String address = "";


    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOpen = findViewById(R.id.open_picker);


        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,LocationPicker.class);
                locationPickerActivityResultLauncher.launch(intent);
            }
        });


    }
    private ActivityResultLauncher<Intent> locationPickerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    TextView TXVW = findViewById(R.id.addressShow);
                    Log.d(TAG, "onActivityResult: ");
                    if(result.getResultCode() == Activity.RESULT_OK){

                        Intent data = result.getData();
                        if(data != null){
                            latitude = data.getDoubleExtra("latitude", 0.0);
                            longitude = data.getDoubleExtra("longitude", 0.0);
                            address = data.getStringExtra("address");

                            Log.d(TAG, "onActivityResult: latitude: " + latitude);
                            Log.d(TAG, "onActivityResult: longitude: " + longitude);


                            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());


                            try {
                                List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                                String addres = addressList.get(0).getAddressLine(0);
                                Log.d(TAG, "onPlaceSelected: Adress: "+ addres);
                                TXVW.setText(addres);

                            } catch (IOException e) {

                                throw new RuntimeException(e);
                            }
                            Log.d(TAG, "onActivityResult: address: " + address);

                        }
                    } else {
                        Log.d(TAG, "onActivityResult: Cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled!",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
    );
}