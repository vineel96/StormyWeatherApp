package com.example.vineelabhinav.stormy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements LocationListener{

    public static final String TAG=MainActivity.class.getSimpleName();
    private CurrentWeather mCurrentWeather;
    private Double latitude;
    private Double longitude;
    private String locality;

    private String provider;
    private LocationManager mLocationManager;
    Criteria criteria;

    AlertDialog dialog;


    @BindView(R.id.timeLabel) TextView mTimeLabel;
    @BindView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.refreshImageView)ImageView mRefreshImageView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.locationLabel) TextView mLocationLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mProgressBar.setVisibility(View.INVISIBLE);


        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isNetworkAvailable()) {
                    getLocation();
                    if(latitude!=null && longitude!=null) {
                        getLocalityName(latitude, longitude);
                        getForecast(latitude, longitude);
                    }
                }
                else
                    Toast.makeText(MainActivity.this, R.string.network_unavailable_message, Toast.LENGTH_LONG).show();
            }
        });
        if(isNetworkAvailable()) {
            getLocation();
            if(latitude!=null && longitude!=null) {
                getLocalityName(latitude, longitude);
                getForecast(latitude, longitude);
            }
        }
        else
            Toast.makeText(MainActivity.this, R.string.network_unavailable_message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.d(TAG,"In OnResume Method");
        if(dialog!=null) {
            dialog.dismiss();
            dialog = null;
        }
        if(provider!=null && mLocationManager!=null) {
           mLocationManager.requestLocationUpdates(provider, 400, 1, this);
            getLocation();
            if(latitude!=null && longitude!=null) {
                getLocalityName(latitude, longitude);
                getForecast(latitude, longitude);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"In OnPause Method");
        if(mLocationManager!=null)
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = (location.getLatitude());
        longitude = (location.getLongitude());
        Log.d(TAG, "Latitude: " + latitude + ", Longitude: " + longitude);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        Toast.makeText(this, "Enabled New Provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(this, provider+ " Has Been Disabled ",
                Toast.LENGTH_SHORT).show();
        TurnGPS();
    }

    /* Gives an Alert Dialog Which Asks User To Turn On GPS */
    public void TurnGPS() {
        try {
           AlertDialog.Builder builder = new AlertDialog.Builder(this);
            Log.d(TAG, "FUCK3");
            builder.setTitle("Current Location").setMessage("Enable access my location under my location under Settings");
            builder.setPositiveButton("SETTINGS", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            Log.d(TAG,"In TurnGPS Method");
            dialog = builder.create();
            dialog.show();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /* Gives The Locality Name Of Given Latitude And Longitude */
    public void getLocalityName(Double latitude,Double longitude) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            Address obj=addresses.get(0);
            Log.d(TAG,obj.getAdminArea()+",,"+obj.getSubAdminArea()+",,"+obj.getLocality());
            locality=obj.getLocality();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

     /* This Function Makes an API Call to Darksky Forecast To Fetch Current Weather Data */
    public void getForecast(double latitude,double longitude) {
        String apiKey="dc5ab389de96c9861869ba1e4606733b";  /* API Key for Weather Forecast */

        String forecastUrl="https://api.darksky.net/forecast/"+apiKey+"/"+latitude+","+longitude;

        if(isNetworkAvailable()) {
            toggleRefresh();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(forecastUrl).build();
            Call call = client.newCall(request);

        /*  Asynchronous call */
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData=response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather=getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay(mCurrentWeather);
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception Caught:", e);
                        //e.printStackTrace();
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "Exception Caught:", e);
                    }
                }
            });
        }
        else
            Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_LONG).show();

        /*   SYNCHRONOUS NETWORK CALL  */
        /*try {
            Response response=call.execute();
            if(response.isSuccessful())
                Log.v(TAG,response.body().string());
        } catch (IOException e) {
            Log.e(TAG,"Exception Caught:",e);
            //e.printStackTrace();
        }*/

    }

    /* Sets On And Off Progress Bar and Refresh Button While Recieving Data */
    public void toggleRefresh() {

        if(mProgressBar.getVisibility()==View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else
        {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    /*Updates the UI With Current Weather Data */
    public void updateDisplay(CurrentWeather currentWeather) {
        mTemperatureLabel.setText(currentWeather.getTemperature()+ "");
        mTimeLabel.setText("At " + mCurrentWeather.getFormattedTime() + " it will be");
        mHumidityValue.setText(mCurrentWeather.getHumidity()+ "");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance()+"%");
        mSummaryLabel.setText(mCurrentWeather.getSummary());
        mLocationLabel.setText(locality);
        Drawable drawable=getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    /* Sets The CurrentWeather Object With Appropriate Data */
    public CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast=new JSONObject(jsonData);
        String timezone=forecast.getString("timezone");
        Log.i(TAG,"From Json"+timezone);

        JSONObject currently=forecast.getJSONObject("currently");

        CurrentWeather currentWeather=new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);

        Log.d(TAG,currentWeather.getFormattedTime());

        return currentWeather;
    }

    /* Checks if Mobile Has Internet ON */
    public boolean isNetworkAvailable() {
        ConnectivityManager manager= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=manager.getActiveNetworkInfo();
        boolean isAvailabe=false;
        if(networkInfo!=null && networkInfo.isConnected())
            isAvailabe=true;
        return isAvailabe;
    }

    /*Alerts User About Any Netwok issues */
    public void alertUserAboutError() {
        AlertDialogFragment dialog=new AlertDialogFragment();
        dialog.show(getFragmentManager(),"error_dialog");
    }

    /* Gets The Present Location's Latitude And Longitude */
    public void getLocation() {

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        provider = LocationManager.NETWORK_PROVIDER;

        Location location = mLocationManager.getLastKnownLocation(provider);
        Log.d(TAG,"In getLocation");

        if (location != null) {
            System.out.println("Provider " + provider + " Has Been Selected.");
            onLocationChanged(location);
        } else {
            Log.d(TAG, "Location Not Available");
            TurnGPS();
        }
    }
}
