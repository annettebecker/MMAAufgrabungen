package com.example.aufgrabungsapp;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.SyncStateContract;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.esri.core.geometry.Point;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;

/**
 * Created by Annette on 21.06.2017.
 * according : https://developer.android.com/training/location/display-address.html
 */

public class AddressIntentService extends IntentService {

    protected ResultReceiver mReceiver;

    /*
     * Creates an IntentService
     */
    public AddressIntentService(){
        super("AdressInt");
    }

    /**
     * find address
     * @param intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Log.wtf(TAG, "No receiver received. There is nowhere to send the results.");
            return;
        }


        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        // read address
        String givenAddress = (String) intent.getStringExtra(Constants.LOCATION_DATA_EXTRA);


        List<Address> coordinates = null;
        try {
            //get coordinates
            coordinates = geocoder.getFromLocationName(givenAddress, 1);

        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.ErrorGeocoder);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.ErrorAddress);
            Log.e(TAG, errorMessage, illegalArgumentException);
        }


        // Handle case where no address was found.
        if (coordinates == null || coordinates.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.ErrorCoordinates);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        }
        else {
            Log.i(TAG, "get Coordinates");
            // Read Coordinates
            double latitude= coordinates.get(0).getLatitude();
            double longitude= coordinates.get(0).getLongitude();
            // Fetch the address lines using getAddressLine,
            // join them, and save them
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i <= coordinates.get(0).getMaxAddressLineIndex()-1; i++) { //-1 > no country
                builder.append((coordinates.get(0).getAddressLine(i)) + " ");
            }
            String address = builder.toString();
            JSONObject result = new JSONObject();

            try {
                result.put(Constants.LATITUDE, latitude);
                result.put(Constants.LONGITUDE, longitude);
                result.put(Constants.ADDRESS, address);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // Send Result back
            deliverResultToReceiver(Constants.SUCCESS_RESULT, result.toString());
        }


    }

    /**
     * send back result
     * @param resultCode
     * @param message
     */
    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
}
