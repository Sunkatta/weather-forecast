package com.example.weatherforecast.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.weatherforecast.Common.Constants;
import com.example.weatherforecast.Entities.Forecast;
import com.example.weatherforecast.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

public class DailyForecast extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_forecast);

        LinearLayout detailedForecastContainerWrapper = findViewById(R.id.detailedForecastContainerWrapper);
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            int index = bundle.getInt("index");
            for (int i = index; i < index + Constants.NUMBER_OF_FORECASTS_IN_24_HOURS; i ++) {
                try {
                    JSONObject detailedForecast = new JSONObject(bundle.getString(Integer.toString(i)));

                    LinearLayout detailedForecastContainer = new LinearLayout(getApplicationContext());
                    detailedForecastContainer.setOrientation(LinearLayout.HORIZONTAL);

                    TextView weatherConditionView = new TextView(getApplicationContext());
                    weatherConditionView.setGravity(Gravity.RIGHT);
                    weatherConditionView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    weatherConditionView.setPadding(5,25,20,0);
                    weatherConditionView.setTextColor(Color.WHITE);

                    Date date = new Date(detailedForecast.getLong(Constants.FORECAST_DATE_TIME) * 1000);
                    String temperature = detailedForecast.getJSONObject(Constants.MAIN_FORECAST_INFORMATION).getString(Constants.TEMPERATURE);
                    JSONObject weatherCondition = (JSONObject)detailedForecast.getJSONArray(Constants.WEATHER).get(0);
                    String weatherStatus = weatherCondition.getString(Constants.DETAILED_WEATHER_STATUS);
                    String iconCode = weatherCondition.getString(Constants.ICON);

                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, date.getHours());
                    Integer time = calendar.get(Calendar.HOUR_OF_DAY);

                    Forecast forecast = new Forecast(
                            temperature,
                            "",
                            weatherStatus,
                            0,
                            time,
                            iconCode,
                            90,
                            null,
                            null
                    );

                    ImageView imageView = new ImageView(getApplicationContext());
                    imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    imageView.setImageBitmap(forecast.getBitmap());

                    TextView temperatureView = new TextView(getApplicationContext());
                    temperatureView.setGravity(Gravity.CENTER_HORIZONTAL);
                    temperatureView.setTextColor(Color.WHITE);

                    TextView timeView = new TextView(getApplicationContext());
                    timeView.setGravity(Gravity.CENTER_HORIZONTAL);
                    timeView.setTextColor(Color.WHITE);

                    timeView.setText(forecast.getTimeOfDay() + ": ");
                    temperatureView.setText(forecast.getTemperature());
                    weatherConditionView.setText(forecast.getWeatherStatus().substring(0, 1).toUpperCase() + forecast.getWeatherStatus().substring(1));

                    detailedForecastContainer.addView(imageView);
                    detailedForecastContainer.addView(timeView);
                    detailedForecastContainer.addView(temperatureView);
                    detailedForecastContainer.addView(weatherConditionView);
                    detailedForecastContainerWrapper.addView(detailedForecastContainer);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
