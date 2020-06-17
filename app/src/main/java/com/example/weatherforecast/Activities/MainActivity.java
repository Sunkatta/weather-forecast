package com.example.weatherforecast.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.weatherforecast.Common.Constants;
import com.example.weatherforecast.Entities.Forecast;
import com.example.weatherforecast.R;
import com.example.weatherforecast.Services.FetchAddressIntentService;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    protected EditText cityInput;
    protected TextView city;
    protected Button getForecastBtn;
    protected LinearLayout weatherContainer;
    protected LinearLayout currentWeatherContainer;

    private ResultReceiver resultReceiver;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultReceiver = new AddressResultReceiver(new Handler());

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_CODE_LOCATION_PERMISSION
            );
        } else {
            getCurrentLocation();
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.custom_toolbar);

        weatherContainer = findViewById(R.id.weatherContainer);
        currentWeatherContainer = findViewById(R.id.currentWeatherContainer);

        city = findViewById(R.id.city);
        getForecastBtn = findViewById(R.id.getForecastBtn);
        cityInput = findViewById(R.id.cityInput);

        try {
            initDb();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

        getForecastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                weatherContainer.removeAllViews();
                currentWeatherContainer.removeAllViews();

                final String cityTitle = cityInput.getText().toString();

                try {
                    getForecastData(weatherContainer, currentWeatherContainer, cityTitle);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(MainActivity.this).requestLocationUpdates(
                locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MainActivity.this).removeLocationUpdates(this);

                        if (locationResult != null && locationResult.getLocations().size() > 0) {
                            int latestLocationIndex = locationResult.getLocations().size() - 1;
                            double latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                            double longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();

                            Location location = new Location("providerNA");
                            location.setLatitude(latitude);
                            location.setLongitude(longitude);
                            fetchAddressFromLatLong(location);
                        }
                    }
                }, Looper.getMainLooper());
    }

    private void fetchAddressFromLatLong(Location location) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        startService(intent);
    }

    protected void initDb() throws SQLException {
        SQLiteDatabase db;
        db = SQLiteDatabase.openOrCreateDatabase(
                getFilesDir().getPath() + "/" + "queries.db",
                null
        );

        String q = "CREATE TABLE if not exists QUERIES(";
        q += " ID integer primary key AUTOINCREMENT, ";
        q += " url text not null, ";
        q += " statusCode number not null );";

        db.execSQL(q);
        db.close();
    }

    private void getForecastData(final LinearLayout weatherContainer, final LinearLayout currentWeatherContainer, final String cityTitle) {
        Thread thread = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                try {
                    String result = getForecast(cityTitle);

                    if (result.equals("NOT FOUND")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                city.setText(R.string.city_not_found);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                city.setText(cityTitle.substring(0, 1).toUpperCase() + cityTitle.substring(1));
                            }
                        });
                        JSONObject jsonObject = (JSONObject) new JSONTokener(result).nextValue();
                        JSONArray weatherInfo = jsonObject.getJSONArray("list");

                        loadUI(weatherInfo, weatherContainer, currentWeatherContainer);
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getForecast(String city) throws IOException {
        String result = "";
        city = URLEncoder.encode(city, "UTF-8");

        URL url = new URL("http://api.openweathermap.org/data/2.5/forecast?q=" + city + "&units=metric&appid=f4b567bc5df788c9aec3f07b7570e1bc");

        HttpURLConnection client = (HttpURLConnection) url.openConnection();
        client.setRequestMethod("GET");
        client.setDoOutput(true);
        client.setDoInput(true);

        OutputStream outputStream = client.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        writer.flush();
        writer.close();
        outputStream.close();

        int responseCode = client.getResponseCode();

        saveRequestToDb(url, responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String line = "";
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));

            while ((line = bufferedReader.readLine()) != null) {
                result += line;
                result += "\n";
            }
        } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            result = "NOT FOUND";
        } else {
            result = "{'error': 'HTTP Response code: " + responseCode + "'}";
        }

        return result;
    }

    private void saveRequestToDb(URL url, Integer statusCode) {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(
                    getFilesDir().getPath() + "/" + "queries.db",
                    null
            );

            String q = "INSERT INTO QUERIES (url, statusCode) ";
            q += "VALUES(?, ?);";

            db.execSQL(q, new Object[] { url.toURI(), statusCode });
        } catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } finally {
            if (db != null) {
                db.close();
                db = null;
            }
        }
    }

    private void loadUI(final JSONArray weatherInfo, final LinearLayout weatherContainer, final LinearLayout currentWeatherContainer) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < weatherInfo.length(); i+= Constants.NUMBER_OF_FORECASTS_IN_24_HOURS) {
                    LinearLayout cardView = new LinearLayout(getApplicationContext());
                    cardView.setOrientation(LinearLayout.VERTICAL);
                    cardView.setPadding(15, 15, 15, 15);

                    TextView dateView = new TextView(getApplicationContext());
                    dateView.setGravity(Gravity.CENTER_HORIZONTAL);
                    dateView.setTextColor(Color.WHITE);

                    TextView temperatureView = new TextView(getApplicationContext());
                    temperatureView.setGravity(Gravity.CENTER_HORIZONTAL);
                    temperatureView.setTextColor(Color.WHITE);

                    TextView weatherConditionView = new TextView(getApplicationContext());
                    weatherConditionView.setGravity(Gravity.CENTER_HORIZONTAL);
                    weatherConditionView.setTextColor(Color.WHITE);

                    TextView readMore = new TextView(getApplicationContext());
                    readMore.setGravity(Gravity.CENTER_HORIZONTAL);
                    readMore.setTextColor(Color.WHITE);

                    try {
                        JSONObject dailyForecast = (JSONObject)weatherInfo.get(i);
                        Date date = new Date(dailyForecast.getLong(Constants.FORECAST_DATE_TIME) * 1000);
                        String temperature = dailyForecast.getJSONObject(Constants.MAIN_FORECAST_INFORMATION).getString(Constants.TEMPERATURE);
                        JSONObject weatherCondition = (JSONObject)dailyForecast.getJSONArray(Constants.WEATHER).get(0);
                        String weatherStatus = weatherCondition.getString(Constants.GENERAL_WEATHER_STATUS);
                        String iconCode = weatherCondition.getString(Constants.ICON);

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(date);
                        Integer day = calendar.get(Calendar.DAY_OF_WEEK);

                        Forecast forecast = new Forecast(
                                temperature,
                                "",
                                weatherStatus,
                                day,
                                0,
                                iconCode,
                                60,
                                null,
                                null
                        );

                        ImageView imageView = new ImageView(getApplicationContext());
                        imageView.setImageBitmap(forecast.getBitmap());

                        dateView.setText(forecast.getDayOfWeek());
                        temperatureView.setText(forecast.getTemperature());
                        weatherConditionView.setText(forecast.getWeatherStatus());
                        readMore.setText(R.string.read_more);

                        cardView.addView(imageView);
                        cardView.addView(dateView);
                        cardView.addView(temperatureView);
                        cardView.addView(weatherConditionView);
                        cardView.addView(readMore);
                        weatherContainer.addView(cardView);

                        final int index = i;

                        cardView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MainActivity.this, DailyForecast.class);
                                Bundle bundle = new Bundle();

                                bundle.putInt("index", index);

                                for (int j = index; j < index + Constants.NUMBER_OF_FORECASTS_IN_24_HOURS; j++) {
                                    try {
                                        bundle.putString(Integer.toString(j), weatherInfo.get(j).toString());
                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }

                                intent.putExtras(bundle);
                                startActivityForResult(intent, 200, bundle);
                            }
                        });
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                getCurrentForecast(weatherInfo, currentWeatherContainer);
            }
        });
    }

    private void getCurrentForecast(JSONArray weatherInfo, LinearLayout currentWeatherContainer) {
        try {
            ImageView imageView = new ImageView(getApplicationContext());
            LinearLayout cardView = new LinearLayout(getApplicationContext());
            cardView.setOrientation(LinearLayout.VERTICAL);
            cardView.setPadding(15, 15, 15, 15);

            TextView weatherConditionView = new TextView(getApplicationContext());
            weatherConditionView.setGravity(Gravity.CENTER_HORIZONTAL);
            weatherConditionView.setTextColor(Color.WHITE);
            weatherConditionView.setTextSize(24);

            TextView minMaxTemperatureView = new TextView(getApplicationContext());
            minMaxTemperatureView.setGravity(Gravity.CENTER_HORIZONTAL);
            minMaxTemperatureView.setTextColor(Color.WHITE);
            minMaxTemperatureView.setTextSize(24);

            TextView feelsLikeTemperatureView = new TextView(getApplicationContext());
            feelsLikeTemperatureView.setGravity(Gravity.CENTER_HORIZONTAL);
            feelsLikeTemperatureView.setTextColor(Color.WHITE);
            feelsLikeTemperatureView.setTextSize(24);

            JSONObject currentWeather = (JSONObject)weatherInfo.get(0);
            JSONObject weatherCondition = (JSONObject)currentWeather.getJSONArray(Constants.WEATHER).get(0);
            String weatherStatus = weatherCondition.getString(Constants.DETAILED_WEATHER_STATUS);
            String iconCode = weatherCondition.getString(Constants.ICON);

            Forecast forecast = new Forecast(
                    "",
                    currentWeather.getJSONObject(Constants.MAIN_FORECAST_INFORMATION).getString(Constants.FEELS_LIKE),
                    weatherStatus,
                    0,
                    0,
                    iconCode,
                    200,
                    currentWeather.getJSONObject(Constants.MAIN_FORECAST_INFORMATION).getString(Constants.MIN_TEMPERATURE),
                    currentWeather.getJSONObject(Constants.MAIN_FORECAST_INFORMATION).getString(Constants.MAX_TEMPERATURE)
            );

            imageView.setImageBitmap(forecast.getBitmap());
            minMaxTemperatureView.setText(forecast.getMinTemperature() + "/" + forecast.getMaxTemperature());
            feelsLikeTemperatureView.setText("Feels like: " + forecast.getFeelsLikeTemperature());
            weatherConditionView.setText(forecast.getWeatherStatus().substring(0, 1).toUpperCase() + forecast.getWeatherStatus().substring(1));

            cardView.addView(imageView);
            cardView.addView(weatherConditionView);
            cardView.addView(minMaxTemperatureView);
            cardView.addView(feelsLikeTemperatureView);
            currentWeatherContainer.addView(cardView);
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            if (resultCode == Constants.SUCCESS_RESULT) {
                String[] location = Objects.requireNonNull(resultData.getString(Constants.RESULT_DATA_KEY)).trim().split(",");
                String city = location[location.length - 2].trim();
                getForecastData(weatherContainer, currentWeatherContainer, city);
            } else {
                Toast.makeText(MainActivity.this, resultData.getString(Constants.RESULT_DATA_KEY), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
