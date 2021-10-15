package com.dvt.weatherapp.utils;

import com.dvt.weatherapp.model.db.CurrentWeather;
import com.dvt.weatherapp.model.db.FavoriteWeather;
import com.dvt.weatherapp.model.db.FiveDayWeather;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class DbUtil {


    /**
     * Get query of currentWeatherBox
     *
     * @param currentWeatherBox instance of {@link Box<CurrentWeather>}
     * @return instance of {@link Query<CurrentWeather>}
     */
    public static Query<CurrentWeather> getCurrentWeatherQuery(Box<CurrentWeather> currentWeatherBox) {
        return currentWeatherBox.query().build();
    }

    /**
     * Get query of fiveDayWeatherBox
     *
     * @param fiveDayWeatherBox instance of {@link Box<FiveDayWeather>}
     * @return instance of {@link Query<FiveDayWeather>}
     */
    public static Query<FiveDayWeather> getFiveDayWeatherQuery(Box<FiveDayWeather> fiveDayWeatherBox) {
        return fiveDayWeatherBox.query().build();
    }


    /**
     * Get query of favoriteWeatherBox
     *
     * @param favoriteWeatherBox instance of {@link Box<FavoriteWeather>}
     * @return instance of {@link Query<FavoriteWeather>}
     */
    public static Query<FavoriteWeather> getFavoriteWeatherQuery(Box<FavoriteWeather> favoriteWeatherBox) {
        return favoriteWeatherBox.query().build();
    }
}
