package com.dvt.weatherapp.ui.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import com.dvt.weatherapp.databinding.ActivityHourlyBinding;
import com.dvt.weatherapp.model.db.FiveDayWeather;
import com.dvt.weatherapp.model.db.ItemHourlyDB;
import com.dvt.weatherapp.utils.AppUtil;
import com.dvt.weatherapp.utils.Constants;
import com.dvt.weatherapp.utils.ElasticDragDismissFrameLayout;
import com.dvt.weatherapp.utils.MyApplication;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public class HourlyActivity extends BaseActivity {
    private FastAdapter<ItemHourlyDB> mFastAdapter;
    private ItemAdapter<ItemHourlyDB> mItemAdapter;
    private FiveDayWeather fiveDayWeather;
    private Box<ItemHourlyDB> itemHourlyDBBox;
    private Typeface typeface;
    private ActivityHourlyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHourlyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setVariables();

        setupDismissFrameLayout();
    }

    private void setupDismissFrameLayout() {
        binding.draggableFrame.addListener(new ElasticDragDismissFrameLayout.SystemChromeFader(this) {
            @Override
            public void onDragDismissed() {
                super.onDragDismissed();
                finishAfterTransition();
            }
        });
    }

    private void setVariables() {
        Intent intent = getIntent();
        fiveDayWeather = intent.getParcelableExtra(Constants.FIVE_DAY_WEATHER_ITEM);
        BoxStore boxStore = MyApplication.getBoxStore();
        itemHourlyDBBox = boxStore.boxFor(ItemHourlyDB.class);
        binding.cardView.setCardBackgroundColor(fiveDayWeather.getColor());
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(fiveDayWeather.getDt() * 1000L);

        binding.dayNameTextView.setText(Constants.DAYS_OF_WEEK[calendar.get(Calendar.DAY_OF_WEEK) - 1]);

        if (fiveDayWeather.getMaxTemp() < 0 && fiveDayWeather.getMaxTemp() > -0.5) {
            fiveDayWeather.setMaxTemp(0);
        }
        if (fiveDayWeather.getMinTemp() < 0 && fiveDayWeather.getMinTemp() > -0.5) {
            fiveDayWeather.setMinTemp(0);
        }
        if (fiveDayWeather.getTemp() < 0 && fiveDayWeather.getTemp() > -0.5) {
            fiveDayWeather.setTemp(0);
        }
        binding.tempTextView.setText(String.format(Locale.getDefault(), "%.0f°", fiveDayWeather.getTemp()));
        binding.minTempTextView.setText(String.format(Locale.getDefault(), "%.0f°", fiveDayWeather.getMinTemp()));
        binding.maxTempTextView.setText(String.format(Locale.getDefault(), "%.0f°", fiveDayWeather.getMaxTemp()));
        binding.animationView.setAnimation(AppUtil.getWeatherAnimation(fiveDayWeather.getWeatherId()));
        binding.animationView.playAnimation();
        typeface = Typeface.createFromAsset(getAssets(), "fonts/Vazir.ttf");
    }


}
