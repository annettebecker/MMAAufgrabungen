package com.example.aufgrabungsapp;


import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.ogc.WMSLayer;
import com.esri.android.map.ogc.WMTSLayer;
import com.esri.core.geodatabase.ShapefileFeatureTable;
import com.esri.core.geometry.AngularUnit;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.SimpleRenderer;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.SpatialRelationship;
import com.esri.core.tasks.query.QueryParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import static com.esri.core.geometry.GeometryEngine.project;

public class MainActivity extends AppCompatActivity implements AsyncResponse{

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final long MIN_TIME_BW_UPDATES = 10;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 100;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    private MapView mMapView;
    private DownloadUnzipFile df;
    ShapefileFeatureTable shapefileTable;
    FeatureLayer featureLayer;
    private Button button;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location location;
    private AddressResultReceiver mResultReceiver;
    private GraphicsLayer resultGeomLayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mResultReceiver = new AddressResultReceiver(new Handler());

        button = (Button) findViewById(R.id.button2);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        mMapView = (MapView) findViewById(R.id.map);
        //EPSG 4326
/*        ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer("https://services.arcgisonline.com/arcgis/rest/services/ESRI_StreetMap_World_2D/MapServer");

        mMapView.addLayer(tiledLayer);
*/
        String wmsURL = "http://www.geodatenzentrum.de/xml/WMS_webatlasde.light_grau.xml?";

        SpatialReference sp = SpatialReference.create(32632);
        WMSLayer wmsLayer = new WMSLayer(wmsURL, sp);
        wmsLayer.setMaxScale(8500);
        mMapView.addLayer(wmsLayer);


        mMapView.zoomTo(new Point(406468.92, 5757466.96), 5);
        mMapView.setResolution(23);

        resultGeomLayer = new GraphicsLayer();
        mMapView.addLayer(resultGeomLayer);

        loadAndDisplayAufgrabungen();
    }




    //************************ Load and Display Aufgrabungen ***************************

    /**
     * after verify Permissions, download Shape.zip
     */
    private void loadAndDisplayAufgrabungen() {
        if (verifyStoragePermissions(this)) {
            df = new DownloadUnzipFile();
            df.delegatResult = this;
            df.execute(this);
        }
    }

    /**
     * display aufgrabungen
     * @param output
     */
    @Override
    public void downloadFinish(String output) {
        try {
            //get Path
            String shapeData = output;
            //display
            shapefileTable = new ShapefileFeatureTable(shapeData);
            featureLayer = new FeatureLayer(shapefileTable);
            SimpleLineSymbol polygonOutline = new SimpleLineSymbol(Color.RED, 2, SimpleLineSymbol.STYLE.SOLID);
            featureLayer.setRenderer(new SimpleRenderer(new SimpleFillSymbol(Color.RED).setOutline(polygonOutline)));

            featureLayer.setMaxScale(0);
            featureLayer.setMinScale(300000);
            featureLayer.setVisible(true);

            mMapView.addLayer(featureLayer);

            setListenerToGetFeatureInfo();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the app has permission to write to device storage     *
     * @param activity
     * according: https://stackoverflow.com/questions/33719170/android-6-0-file-write-permission-denied
     */
    public static boolean verifyStoragePermissions(Activity activity) {
        // Check if permissions are given
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // Ask the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        }
        return true;
    }


    /**
     * if new permission sets
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //check permissions
        boolean in = false;
        int position = -1;
        String permission = "";
        for(int i = 0; i < permissions.length; i++){
            if(permissions[i].equals("android.permission.WRITE_EXTERNAL_STORAGE") ){
                permission = "WRITE_EXTERNAL_STORAGE";
                in = true;
                position = i;
                break;
            }
        }
        if(permission.equals("WRITE_EXTERNAL_STORAGE") && in == true && grantResults[position] == 0){
            loadAndDisplayAufgrabungen();
        }
        else if (permission.equals("WRITE_EXTERNAL_STORAGE")){
            Toast toast = Toast.makeText(getApplicationContext(), "Berechtigung zum Speichern von Inhalten fehlt", Toast.LENGTH_SHORT);
            toast.show();
        }

    }






    //************************ FeatureInfo Aufgrabungen ***************************

    /**
     * Listener for featureInfo
     * according: https://github.com/Esri/arcgis-runtime-samples-android/blob/master/feature-layer-show-attributes/src/main/java/com/esri/arcgisruntime/sample/featurelayershowattributes/MainActivity.java
     */
    private void setListenerToGetFeatureInfo() {

        // set listener
        mMapView.setOnTouchListener(new MapOnTouchListener(this,mMapView) {

            @Override
            public boolean onSingleTap(final MotionEvent point){
                //Delete existing Fragments
                deleteFragment();

                featureLayer.clearSelection();

                //Zoom to touchPoint
                final Point mapPoint = mMapView.toMapPoint(new Point(point.getX(), point.getY()));
                mMapView.zoomTo(mapPoint,1);


                //Define searchGeometry
                Polygon searchGeometry = GeometryEngine.buffer(mapPoint, mMapView.getSpatialReference(),  30, Unit.create(LinearUnit.Code.METER));

                //QueryParameters define
                QueryParameters queryAufbrueche = new QueryParameters();
                queryAufbrueche.setReturnGeometry(true);
                queryAufbrueche.setInSpatialReference(mMapView.getSpatialReference());
                queryAufbrueche.setGeometry(searchGeometry);
                queryAufbrueche.setSpatialRelationship(SpatialRelationship.INTERSECTS);

                //Start selection
                featureLayer.selectFeatures(queryAufbrueche, FeatureLayer.SelectionMode.NEW, new CallbackListener<FeatureResult>() {

                    @Override
                    public void onCallback(FeatureResult objects) {
                        Iterator i = objects.iterator();
                        HashSet elements  = new HashSet();
                        while(i.hasNext()) {
                            Feature feature = (Feature)i.next();
                            elements.add(feature);
                        }
                        if(!elements.isEmpty())
                            addFragment(elements);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("Error Feature Info", throwable.toString());
                    }
                });
                return true;
            }


            /**
             * dynamically add Fragement and fill it with featureinfos
             * according: http://wptrafficanalyzer.in/blog/dynamically-add-fragments-to-an-activity-in-android/
             */
            private void addFragment(HashSet features) {
                //Add new Fragment to Activity
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                FeatureInfoFragment fragment = new FeatureInfoFragment();
                //Send Bundle with Data
                Bundle data = new Bundle();
                data.putSerializable("features", (Serializable) features);

                fragment.setArguments(data);

                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

                // Commit the transaction
                fragmentTransaction.add(R.id.fragment_container, fragment, "featureInfo").addToBackStack(null).commit();
            }

            private void deleteFragment() {
                //Delete Fragment from Activity
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                FeatureInfoFragment fragment = (FeatureInfoFragment) fragmentManager.findFragmentByTag("featureInfo");
                //Delete Fragment
                if(fragment != null) {
                    fragmentTransaction.remove(fragment);
                    fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                }

                // Commit the transaction
                fragmentTransaction.commit();
            }

        });

    }


    /**
     * Override > features not longer selected after cancel fragment
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            int count = getFragmentManager().getBackStackEntryCount();
            if (count == 0) {
                super.onBackPressed();
            } else {
                featureLayer.clearSelection();
            }
        }
        return true;
    }





    //************************ GPS ***************************

    /**
     * onClick method for Button 'Standort'
     */
    public void onClickGPS(View v) {
        // instantiate the location manager, note you will need to request permissions in your manifest
        //LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // get the last know location from your location manager.

        // Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        // now get the lat/lon from the location and do something with it.
        //nowDoSomethingWith(location.getLatitude(), location.getLongitude());
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast toast = Toast.makeText(getApplicationContext(), "Bitte zunächst GPS aktivieren", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
           if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);
            }
            else {
               resultGeomLayer.removeAll();

               double actualLongitude = getLocation().getLongitude();
               double actualLatitude = getLocation().getLatitude();

               SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(Color.BLUE, 12, SimpleMarkerSymbol.STYLE.CIRCLE);
               Point pointLocation = new Point(actualLongitude, actualLatitude);

               SpatialReference spacRef = SpatialReference.create(4326);
               Point projPoint = (Point) project(pointLocation,spacRef,mMapView.getSpatialReference());


               Context context = getApplicationContext();
               CharSequence text =   projPoint.getX() + ", " + projPoint.getY();
               int duration = Toast.LENGTH_SHORT;

               Toast toast = Toast.makeText(context, text, duration);
               toast.show();

               // create a point marker on gps location
               Graphic pointGraphic = new Graphic(projPoint, symbol);
               resultGeomLayer.setVisible(true);
               resultGeomLayer.setMaxScale(0);
               resultGeomLayer.setMinScale(0);
               resultGeomLayer.addGraphic(pointGraphic);

               mMapView.zoomTo(projPoint, 1);

           }
        }

    }

    public Location getLocation() {

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);
            }
            boolean canGetLocation = true;
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
            Log.d("Network", "Network Enabled");
            if (locationManager != null) {
                location = locationManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                }
            }

            // if GPS Enabled get lat/long using GPS Service
            if (location == null) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                Log.d("GPS", "GPS Enabled");
                if (locationManager != null) {
                    location = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }




    //************************ Geocoder ***************************

    /**
     * Read Data from AddressBox and send to geocoder
     */
    public void onClickAddress(View v){

        //Hide keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow( this.getCurrentFocus().getWindowToken(), 0);

        //Read Text
        EditText addressBox = (EditText) findViewById(R.id.address);
        String address = addressBox.getText().toString();
        //Send intent
        Intent intent = new Intent(MainActivity.this, AddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, address);
        startService(intent);
    }



    /**
     * Innerclass to receive Result from geocoder
     * according: https://developer.android.com/training/location/display-address.html
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                //Read Data and convert to JSONObject
                String mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
                Point adressPoint = null;
                JSONObject jsonObj = null;
                Point projPointAdress = null;
                try {
                    jsonObj = new JSONObject(mAddressOutput);
                    adressPoint = new Point(jsonObj.getDouble(Constants.LONGITUDE),jsonObj.getDouble(Constants.LATITUDE));
                    SpatialReference spacRef = SpatialReference.create(4326);
                    projPointAdress = GeometryEngine.project(adressPoint.getX(), adressPoint.getY(), mMapView.getSpatialReference());

                    resultGeomLayer.removeAll();
                    SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(Color.BLUE, 12, SimpleMarkerSymbol.STYLE.CIRCLE);
                    Graphic pointGraphic = new Graphic(projPointAdress, symbol);
                    resultGeomLayer.setVisible(true);
                    resultGeomLayer.setMaxScale(0);
                    resultGeomLayer.setMinScale(0);
                    resultGeomLayer.addGraphic(pointGraphic);

                    //Toast success
                    Toast.makeText(MainActivity.this, getString(R.string.adresseErfolgreich) + jsonObj.getString(Constants.ADDRESS), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Zoom to Address
                mMapView.zoomTo(projPointAdress, 1);


                //Delete Text from AddressBox
                EditText addressBox = (EditText)findViewById(R.id.address);
                addressBox.setText("");

            }
            else{
                Toast.makeText(MainActivity.this, resultData.getString(Constants.RESULT_DATA_KEY), Toast.LENGTH_SHORT).show();
            }

        }
    }


}



