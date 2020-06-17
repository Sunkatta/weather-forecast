package com.example.weatherforecast.Entities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import java.net.URL;

public class Forecast {
    private String temperature;
    private String feelsLikeTemperature;
    private String minTemperature;
    private String maxTemperature;
    private String weatherStatus;
    private String dayOfWeek;
    private String timeOfDay;
    private Bitmap bitmap;

    public Forecast(
            String temperature,
            String feelsLikeTemperature,
            String weatherStatus,
            Integer dayOfWeek,
            Integer timeOfDay,
            String iconCode,
            Integer iconDimensions,
            @Nullable String minTemperature,
            @Nullable String maxTemperature
    ) {
        setTemperature(temperature);
        setFeelsLikeTemperature(feelsLikeTemperature);
        setWeatherStatus(weatherStatus);
        this.dayOfWeek = setDayOfWeek(dayOfWeek);
        this.setBitmap(iconCode, iconDimensions);
        setTimeOfDay(timeOfDay);
        setMinTemperature(minTemperature);
        setMaxTemperature(maxTemperature);
    }

    public String getTemperature() {
        return temperature;
    }

    private void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getFeelsLikeTemperature() {
        return feelsLikeTemperature;
    }

    private void setFeelsLikeTemperature(String feelsLikeTemperature) {
        this.feelsLikeTemperature = feelsLikeTemperature;
    }

    public String getMinTemperature() {
        return minTemperature;
    }

    private void setMinTemperature(String minTemperature) {
        this.minTemperature = minTemperature;
    }

    public String getMaxTemperature() {
        return maxTemperature;
    }

    private void setMaxTemperature(String maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public String getWeatherStatus() {
        return weatherStatus;
    }

    private void setWeatherStatus(String weatherStatus) {
        this.weatherStatus = weatherStatus;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    private String setDayOfWeek(Integer dayOfWeek) {
        switch (dayOfWeek) {
            case 1:
                this.dayOfWeek = "Sun";
                break;
            case 2:
                this.dayOfWeek = "Mon";
                break;
            case 3:
                this.dayOfWeek = "Tue";
                break;
            case 4:
                this.dayOfWeek = "Wed";
                break;
            case 5:
                this.dayOfWeek = "Thu";
                break;
            case 6:
                this.dayOfWeek = "Fri";
                break;
            case 7:
                this.dayOfWeek = "Sat";
                break;
            default:
                break;
        }
        return this.dayOfWeek;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    private void setTimeOfDay(Integer timeOfDay) {
        if (timeOfDay < 10) {
            this.timeOfDay = "0" + timeOfDay + ":00";
        } else {
            this.timeOfDay = timeOfDay.toString() + ":00";
        }
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    private void setBitmap(final String iconCode, final Integer iconDimensions) {
        try {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL imageURL = new URL("http://openweathermap.org/img/wn/" + iconCode + "@2x.png");
                        Bitmap icon = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
                        icon = Bitmap.createScaledBitmap(icon, iconDimensions, iconDimensions, false);
                        bitmap = icon;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            thread.start();
            thread.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
