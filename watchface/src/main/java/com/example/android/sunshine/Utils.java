package com.example.android.sunshine;

/**
 * Created by OFFICE on 1/15/2017.
 */

public class Utils {

    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found
        if (weatherId >= 200 && weatherId <= 232) {
            return com.example.android.sunshine.R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return com.example.android.sunshine.R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return com.example.android.sunshine.R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return com.example.android.sunshine.R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return com.example.android.sunshine.R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return com.example.android.sunshine.R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return com.example.android.sunshine.R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return com.example.android.sunshine.R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return com.example.android.sunshine.R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return com.example.android.sunshine.R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return com.example.android.sunshine.R.drawable.ic_cloudy;
        }
        return -1;
    }


}
