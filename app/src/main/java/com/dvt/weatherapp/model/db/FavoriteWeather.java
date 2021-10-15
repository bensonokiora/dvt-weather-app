package com.dvt.weatherapp.model.db;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;

import com.dvt.weatherapp.R;
import com.dvt.weatherapp.databinding.FavoriteItemBinding;
import com.dvt.weatherapp.ui.activity.HourlyActivity;
import com.dvt.weatherapp.ui.activity.MainActivity;
import com.dvt.weatherapp.utils.Constants;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.util.List;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;

@Entity
public class FavoriteWeather extends AbstractItem<FavoriteWeather, FavoriteWeather.MyViewHolder> {
    @Id
    private long id;

    @Unique
    private String name;
    private String country;

    private Double latitude;
    private Double longitude;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @NonNull
    @Override
    public FavoriteWeather.MyViewHolder getViewHolder(@NonNull View v) {
        return new FavoriteWeather.MyViewHolder(v);
    }

    @Override
    public int getType() {
        return R.id.fastadapter_item_adapter;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.multiple_days_item;
    }

    protected static class MyViewHolder extends FastAdapter.ViewHolder<FavoriteWeather> {
        Context context;
        View view;
        FavoriteItemBinding binding;

        MyViewHolder(View view) {
            super(view);
            binding = FavoriteItemBinding.bind(view);
            this.view = view;
            this.context = view.getContext();

        }

        @Override
        public void bindView(@NonNull FavoriteWeather item, @NonNull List<Object> payloads) {
            binding.dateTextView.setText(item.getCountry());
            binding.dayNameTextView.setText(item.getName());

            binding.cardView.setOnClickListener(v -> {
                if (MainActivity.todayFiveDayWeather != null) {
                    Intent intent = new Intent(context, HourlyActivity.class);
                    intent.putExtra(Constants.FIVE_DAY_WEATHER_ITEM, MainActivity.todayFiveDayWeather);
                    context.startActivity(intent);
                }
            });
        }

        @Override
        public void unbindView(@NonNull FavoriteWeather item) {

        }

    }
}
