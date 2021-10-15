package com.dvt.weatherapp.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.dvt.weatherapp.R;
import com.dvt.weatherapp.databinding.ActivityMainBinding;
import com.dvt.weatherapp.model.CityInfo;
import com.dvt.weatherapp.model.currentweather.CurrentWeatherResponse;
import com.dvt.weatherapp.model.daysweather.ListItem;
import com.dvt.weatherapp.model.daysweather.MultipleDaysWeatherResponse;
import com.dvt.weatherapp.model.db.CurrentWeather;
import com.dvt.weatherapp.model.db.FavoriteWeather;
import com.dvt.weatherapp.model.db.FiveDayWeather;
import com.dvt.weatherapp.model.db.ItemHourlyDB;
import com.dvt.weatherapp.model.fivedayweather.FiveDayResponse;
import com.dvt.weatherapp.model.fivedayweather.ItemHourly;
import com.dvt.weatherapp.service.ApiService;
import com.dvt.weatherapp.ui.fragment.AboutFragment;
import com.dvt.weatherapp.ui.fragment.CityInfoFragment;
import com.dvt.weatherapp.ui.fragment.FavoriteFragment;
import com.dvt.weatherapp.utils.ApiClient;
import com.dvt.weatherapp.utils.AppUtil;
import com.dvt.weatherapp.utils.Constants;
import com.dvt.weatherapp.utils.DbUtil;
import com.dvt.weatherapp.utils.MyApplication;
import com.dvt.weatherapp.utils.SnackbarUtil;
import com.dvt.weatherapp.utils.TextViewFactory;
import com.github.pwittchen.prefser.library.rx2.Prefser;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidScheduler;
import io.objectbox.exception.UniqueViolationException;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscriptionList;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.HttpException;

public class MainActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {

    public static final int RC_LOCATION_PERM = 123;
    public static FiveDayWeather todayFiveDayWeather;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private final String defaultLang = "en";
    private final DataSubscriptionList subscriptions = new DataSubscriptionList();
    private FastAdapter<FiveDayWeather> mFastAdapter;
    private ItemAdapter<FiveDayWeather> mItemAdapter;
    private List<FiveDayWeather> fiveDayWeathers;
    private ApiService apiService;
    private Prefser prefser;
    private Box<CurrentWeather> currentWeatherBox;
    private Box<FiveDayWeather> fiveDayWeatherBox;
    private Box<ItemHourlyDB> itemHourlyDBBox;
    private Box<FavoriteWeather> favoriteWeatherBox;
    private boolean isLoad = false;
    private CityInfo cityInfo;
    private String apiKey;
    private Typeface typeface;
    private ActivityMainBinding binding;
    private int[] colors;
    private int[] colorsAlpha;
    private CurrentWeatherResponse _currentWeatherResponse = null;
    private final LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            requestWeatherNow(mLastLocation);
        }
    };
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbarLayout.toolbar);
        initSearchView();
        initValues();
        setupTextSwitchers();
        initRecyclerView();
        showStoredCurrentWeather();
        showStoredFiveDayWeather();
        showStoredFavoriteWeather();
        checkLastUpdate();
    }

    private void initSearchView() {
        binding.toolbarLayout.searchView.setVoiceSearch(false);
        binding.toolbarLayout.searchView.setHint(getString(R.string.search_label));
        binding.toolbarLayout.searchView.setCursorDrawable(R.drawable.custom_curosr);
        binding.toolbarLayout.searchView.setEllipsize(true);
        binding.toolbarLayout.searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                requestWeather(query, true);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        binding.toolbarLayout.searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.toolbarLayout.searchView.showSearch();
            }
        });

    }

    private void initValues() {
        colors = getResources().getIntArray(R.array.mdcolor_500);
        colorsAlpha = getResources().getIntArray(R.array.mdcolor_500_alpha);
        prefser = new Prefser(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        BoxStore boxStore = MyApplication.getBoxStore();
        currentWeatherBox = boxStore.boxFor(CurrentWeather.class);
        fiveDayWeatherBox = boxStore.boxFor(FiveDayWeather.class);
        itemHourlyDBBox = boxStore.boxFor(ItemHourlyDB.class);
        favoriteWeatherBox = boxStore.boxFor(FavoriteWeather.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        binding.swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        binding.swipeContainer.setOnRefreshListener(() -> {
            cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
            if (cityInfo != null) {
                long lastStored = prefser.get(Constants.LAST_STORED_CURRENT, Long.class, 0L);
                if (AppUtil.isTimePass(lastStored)) {
                    requestWeather(cityInfo.getName(), false);
                } else {
                    binding.swipeContainer.setRefreshing(false);
                }
            } else {
                binding.swipeContainer.setRefreshing(false);
            }
        });
        binding.bar.setNavigationOnClickListener(v -> showAboutFragment());
        typeface = Typeface.createFromAsset(getAssets(), "fonts/Vazir.ttf");
        binding.favoriteButton.setOnClickListener(v -> AppUtil.showFragment(new FavoriteFragment(), getSupportFragmentManager(), true));
        binding.contentMainLayout.todayMaterialCard.setOnClickListener(v -> {
            if (todayFiveDayWeather != null) {
                Intent intent = new Intent(MainActivity.this, HourlyActivity.class);
                intent.putExtra(Constants.FIVE_DAY_WEATHER_ITEM, todayFiveDayWeather);
                startActivity(intent);
            }
        });
        binding.toolbarLayout.favorite.setOnClickListener(v -> {
            if (_currentWeatherResponse != null) {
                storeFavoriteWeather(_currentWeatherResponse);
                binding.toolbarLayout.favorite.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.starfilled));

            } else {
                SnackbarUtil
                        .with(binding.swipeContainer)
                        .setMessage(getString(R.string.error_favorite))
                        .setDuration(SnackbarUtil.LENGTH_LONG)
                        .showError();
            }
        });
        binding.toolbarLayout.cityInfo.setOnClickListener(v -> {
            cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
            if (cityInfo != null) {
                showCityInfoFragment(cityInfo);
            } else {
                SnackbarUtil
                        .with(binding.swipeContainer)
                        .setMessage(getString(R.string.error_city_info))
                        .setDuration(SnackbarUtil.LENGTH_LONG)
                        .showError();
            }

        });
    }

    private void setupTextSwitchers() {
        binding.contentMainLayout.tempTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.TempTextView, true, typeface));
        binding.contentMainLayout.tempTextView.setInAnimation(MainActivity.this, R.anim.slide_in_right);
        binding.contentMainLayout.tempTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_left);
        binding.contentMainLayout.descriptionTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.DescriptionTextView, true, typeface));
        binding.contentMainLayout.descriptionTextView.setInAnimation(MainActivity.this, R.anim.slide_in_right);
        binding.contentMainLayout.descriptionTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_left);
        binding.contentMainLayout.minTemp.setFactory(new TextViewFactory(MainActivity.this, R.style.MinTempTextView, false, typeface));
        binding.contentMainLayout.minTemp.setInAnimation(MainActivity.this, R.anim.slide_in_bottom);
        binding.contentMainLayout.minTemp.setOutAnimation(MainActivity.this, R.anim.slide_out_top);
        binding.contentMainLayout.currentTemp.setFactory(new TextViewFactory(MainActivity.this, R.style.CurrentTempTextView, false, typeface));
        binding.contentMainLayout.currentTemp.setInAnimation(MainActivity.this, R.anim.slide_in_bottom);
        binding.contentMainLayout.currentTemp.setOutAnimation(MainActivity.this, R.anim.slide_out_top);
        binding.contentMainLayout.maxTemp.setFactory(new TextViewFactory(MainActivity.this, R.style.MaxTempTextView, false, typeface));
        binding.contentMainLayout.maxTemp.setInAnimation(MainActivity.this, R.anim.slide_in_bottom);
        binding.contentMainLayout.maxTemp.setOutAnimation(MainActivity.this, R.anim.slide_out_top);
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.contentMainLayout.recyclerView.setLayoutManager(layoutManager);
        mItemAdapter = new ItemAdapter<>();
        mFastAdapter = FastAdapter.with(mItemAdapter);
        binding.contentMainLayout.recyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.contentMainLayout.recyclerView.setAdapter(mFastAdapter);
        binding.contentMainLayout.recyclerView.setFocusable(false);
        mFastAdapter.withOnClickListener(new OnClickListener<FiveDayWeather>() {
            @Override
            public boolean onClick(@Nullable View v, @NonNull IAdapter<FiveDayWeather> adapter, @NonNull FiveDayWeather item, int position) {
                Intent intent = new Intent(MainActivity.this, HourlyActivity.class);
                intent.putExtra(Constants.FIVE_DAY_WEATHER_ITEM, item);
                startActivity(intent);
                return true;
            }
        });
    }

    private void showStoredCurrentWeather() {
        Query<CurrentWeather> query = DbUtil.getCurrentWeatherQuery(currentWeatherBox);
        query.subscribe(subscriptions).on(AndroidScheduler.mainThread())
                .observer(new DataObserver<List<CurrentWeather>>() {
                    @Override
                    public void onData(@NonNull List<CurrentWeather> data) {
                        if (data.size() > 0) {
                            hideEmptyLayout();
                            CurrentWeather currentWeather = data.get(0);
                            if (isLoad) {
                                binding.contentMainLayout.tempTextView.setText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getTemp()));
                                binding.contentMainLayout.descriptionTextView.setText(AppUtil.getWeatherStatus(currentWeather.getWeatherId(), AppUtil.isRTL(MainActivity.this)));
                                binding.contentMainLayout.minTemp.setText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getMinTemp()));
                                binding.contentMainLayout.currentTemp.setText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getTemp()));
                                binding.contentMainLayout.maxTemp.setText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getMaxTemp()));

                            } else {
                                binding.contentMainLayout.tempTextView.setCurrentText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getTemp()));
                                binding.contentMainLayout.descriptionTextView.setCurrentText(AppUtil.getWeatherStatus(currentWeather.getWeatherId(), AppUtil.isRTL(MainActivity.this)));
                                binding.contentMainLayout.minTemp.setCurrentText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getMinTemp()));
                                binding.contentMainLayout.currentTemp.setCurrentText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getTemp()));
                                binding.contentMainLayout.maxTemp.setCurrentText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getMaxTemp()));

                            }
                            binding.contentMainLayout.backgroundImage.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, AppUtil.getWeatherBackgroundImage(currentWeather.getWeatherId())));
                        } else {
                            showEmptyLayout();
                        }
                    }
                });
    }

    private void showStoredFavoriteWeather() {
        Query<FavoriteWeather> query = DbUtil.getFavoriteWeatherQuery(favoriteWeatherBox);
        query.subscribe(subscriptions).on(AndroidScheduler.mainThread())
                .observer(new DataObserver<List<FavoriteWeather>>() {
                    @Override
                    public void onData(@NonNull List<FavoriteWeather> data) {
                        if (data.size() > 0) {
                            hideEmptyLayout();
                            // FavoriteWeather currentWeather = data.get(0);


                        }
                    }
                });
    }

    public void showStoredFiveDayWeather() {
        Query<FiveDayWeather> query = DbUtil.getFiveDayWeatherQuery(fiveDayWeatherBox);
        query.subscribe(subscriptions).on(AndroidScheduler.mainThread())
                .observer(new DataObserver<List<FiveDayWeather>>() {
                    @Override
                    public void onData(@NonNull List<FiveDayWeather> data) {
                        if (data.size() > 0) {
                            todayFiveDayWeather = data.remove(0);
                            mItemAdapter.clear();
                            mItemAdapter.add(data);

                        }
                    }
                });
    }

    private void checkLastUpdate() {
        cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
        if (cityInfo != null) {
            binding.toolbarLayout.cityNameTextView.setText(String.format("%s, %s", cityInfo.getName(), cityInfo.getCountry()));
            if (prefser.contains(Constants.LAST_STORED_CURRENT)) {
                long lastStored = prefser.get(Constants.LAST_STORED_CURRENT, Long.class, 0L);
                if (AppUtil.isTimePass(lastStored)) {
                    requestWeather(cityInfo.getName(), false);
                }
            } else {
                requestWeather(cityInfo.getName(), false);
            }
        } else {
            // showEmptyLayout();
            //TODO fix this

            requestWeather();

        }

    }

    public void requestWeather(String cityName, boolean isSearch) {
        if (AppUtil.isNetworkConnected()) {
            getCurrentWeather(cityName, isSearch, this);
            getFiveDaysWeather(cityName);
            binding.toolbarLayout.favorite.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.star));

        } else {
            SnackbarUtil
                    .with(binding.swipeContainer)
                    .setMessage(getString(R.string.no_internet_message))
                    .setDuration(SnackbarUtil.LENGTH_LONG)
                    .showError();
            binding.swipeContainer.setRefreshing(false);
        }
    }

    private void getCurrentWeather(String cityName, boolean isSearch, Context context) {
        apiKey = context.getResources().getString(R.string.open_weather_map_api);
        disposable.add(
                apiService.getCurrentWeather(
                        cityName, Constants.UNITS, defaultLang, apiKey)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<CurrentWeatherResponse>() {
                            @Override
                            public void onSuccess(CurrentWeatherResponse currentWeatherResponse) {
                                isLoad = true;
                                storeCurrentWeather(currentWeatherResponse);
                                storeCityInfo(currentWeatherResponse);
                                _currentWeatherResponse = currentWeatherResponse;
                                binding.swipeContainer.setRefreshing(false);
                                if (isSearch) {
                                    prefser.remove(Constants.LAST_STORED_MULTIPLE_DAYS);
                                }

                            }

                            @Override
                            public void onError(Throwable e) {
                                binding.swipeContainer.setRefreshing(false);
                                try {
                                    HttpException error = (HttpException) e;
                                    handleErrorCode(error);
                                } catch (Exception exception) {
                                    e.printStackTrace();
                                }
                            }
                        })

        );
    }

    private void handleErrorCode(HttpException error) {
        if (error.code() == 404) {
            SnackbarUtil
                    .with(binding.swipeContainer)
                    .setMessage(getString(R.string.no_city_found_message))
                    .setDuration(SnackbarUtil.LENGTH_INDEFINITE)
                    .setAction(getResources().getString(R.string.search_label), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            binding.toolbarLayout.searchView.showSearch();
                        }
                    })
                    .showWarning();

        } else if (error.code() == 401) {
            SnackbarUtil
                    .with(binding.swipeContainer)
                    .setMessage(getString(R.string.invalid_api_key_message))
                    .setDuration(SnackbarUtil.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.ok_label), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    })
                    .showError();

        } else {
            SnackbarUtil
                    .with(binding.swipeContainer)
                    .setMessage(getString(R.string.network_exception_message))
                    .setDuration(SnackbarUtil.LENGTH_LONG)
                    .setAction(getResources().getString(R.string.retry_label), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (cityInfo != null) {
                                requestWeather(cityInfo.getName(), false);
                            } else {
                                binding.toolbarLayout.searchView.showSearch();
                            }
                        }
                    })
                    .showWarning();
        }
    }

    private void showEmptyLayout() {
        Glide.with(MainActivity.this).load(R.drawable.no_city).into(binding.contentEmptyLayout.noCityImageView);
        binding.contentEmptyLayout.emptyLayout.setVisibility(View.VISIBLE);
        binding.contentMainLayout.nestedScrollView.setVisibility(View.GONE);
        binding.contentEmptyLayout.searchTextView.setText(R.string.unknown_location);
        binding.toolbarLayout.favorite.setVisibility(View.GONE);
        binding.toolbarLayout.cityInfo.setVisibility(View.GONE);

    }

    private void hideEmptyLayout() {
        binding.contentEmptyLayout.emptyLayout.setVisibility(View.GONE);
        binding.contentMainLayout.nestedScrollView.setVisibility(View.VISIBLE);
        binding.toolbarLayout.favorite.setVisibility(View.VISIBLE);
        binding.toolbarLayout.cityInfo.setVisibility(View.VISIBLE);

    }

    private void storeCurrentWeather(CurrentWeatherResponse response) {
        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setTemp(response.getMain().getTemp());
        currentWeather.setHumidity(response.getMain().getHumidity());
        currentWeather.setDescription(response.getWeather().get(0).getDescription());
        currentWeather.setMain(response.getWeather().get(0).getMain());
        currentWeather.setWeatherId(response.getWeather().get(0).getId());
        currentWeather.setWindDeg(response.getWind().getDeg());
        currentWeather.setWindSpeed(response.getWind().getSpeed());
        currentWeather.setStoreTimestamp(System.currentTimeMillis());
        currentWeather.setMinTemp(response.getMain().getTempMin());
        currentWeather.setMaxTemp(response.getMain().getTempMax());

        prefser.put(Constants.LAST_STORED_CURRENT, System.currentTimeMillis());
        if (!currentWeatherBox.isEmpty()) {
            currentWeatherBox.removeAll();
            currentWeatherBox.put(currentWeather);
        } else {
            currentWeatherBox.put(currentWeather);
        }
    }

    private void storeFavoriteWeather(CurrentWeatherResponse response) {
        FavoriteWeather favoriteWeather = new FavoriteWeather();


        favoriteWeather.setName(response.getName());
        favoriteWeather.setCountry(response.getSys().getCountry());
        favoriteWeather.setLatitude(response.getCoord().getLat());
        favoriteWeather.setLongitude(response.getCoord().getLon());

        prefser.put(Constants.LAST_STORED_CURRENT, System.currentTimeMillis());
        if (!favoriteWeatherBox.isEmpty()) {
            //favoriteWeatherBox.removeAll();
            try {
                favoriteWeatherBox.put(favoriteWeather);
            } catch (UniqueViolationException e) {

                //Toast.makeText(getApplicationContext(), "City already added to favorites", Toast.LENGTH_SHORT).show();

            }
        } else {
            try {
                favoriteWeatherBox.put(favoriteWeather);
            } catch (UniqueViolationException e) {
                // Toast.makeText(getApplicationContext(), "City already added to favorites", Toast.LENGTH_SHORT).show();


            }
        }
    }

    private void storeCityInfo(CurrentWeatherResponse response) {
        CityInfo cityInfo = new CityInfo();
        cityInfo.setCountry(response.getSys().getCountry());
        cityInfo.setId(response.getId());
        cityInfo.setName(response.getName());
        prefser.put(Constants.CITY_INFO, cityInfo);
        binding.toolbarLayout.cityNameTextView.setText(String.format("%s, %s", cityInfo.getName(), cityInfo.getCountry()));
    }

    private void getFiveDaysWeather(String cityName) {
        disposable.add(
                apiService.getMultipleDaysWeather(
                        cityName, Constants.UNITS, defaultLang, 5, apiKey)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<MultipleDaysWeatherResponse>() {
                            @Override
                            public void onSuccess(MultipleDaysWeatherResponse response) {
                                handleFiveDayResponse(response, cityName);
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                            }
                        })
        );
    }

    private void handleFiveDayResponse(MultipleDaysWeatherResponse response, String cityName) {
        fiveDayWeathers = new ArrayList<>();
        List<ListItem> list = response.getList();
        int day = 0;
        for (ListItem item : list) {
            int color = colors[day];
            int colorAlpha = colorsAlpha[day];
            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
            Calendar newCalendar = AppUtil.addDays(calendar, day);
            FiveDayWeather fiveDayWeather = new FiveDayWeather();
            fiveDayWeather.setWeatherId(item.getWeather().get(0).getId());
            fiveDayWeather.setDt(item.getDt());
            fiveDayWeather.setMaxTemp(item.getTemp().getMax());
            fiveDayWeather.setMinTemp(item.getTemp().getMin());
            fiveDayWeather.setTemp(item.getTemp().getDay());
            fiveDayWeather.setColor(color);
            fiveDayWeather.setColorAlpha(colorAlpha);
            fiveDayWeather.setTimestampStart(AppUtil.getStartOfDayTimestamp(newCalendar));
            fiveDayWeather.setTimestampEnd(AppUtil.getEndOfDayTimestamp(newCalendar));
            fiveDayWeathers.add(fiveDayWeather);
            day++;
        }
        getFiveDaysHourlyWeather(cityName);
    }

    private void getFiveDaysHourlyWeather(String cityName) {
        disposable.add(
                apiService.getFiveDaysWeather(
                        cityName, Constants.UNITS, defaultLang, apiKey)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<FiveDayResponse>() {
                            @Override
                            public void onSuccess(FiveDayResponse response) {
                                handleFiveDayHourlyResponse(response);
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                            }
                        })

        );
    }

    private void handleFiveDayHourlyResponse(FiveDayResponse response) {
        if (!fiveDayWeatherBox.isEmpty()) {
            fiveDayWeatherBox.removeAll();
        }
        if (!itemHourlyDBBox.isEmpty()) {
            itemHourlyDBBox.removeAll();
        }
        for (FiveDayWeather fiveDayWeather : fiveDayWeathers) {
            long fiveDayWeatherId = fiveDayWeatherBox.put(fiveDayWeather);
            ArrayList<ItemHourly> listItemHourlies = new ArrayList<>(response.getList());
            for (ItemHourly itemHourly : listItemHourlies) {
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                calendar.setTimeInMillis(itemHourly.getDt() * 1000L);
                if (calendar.getTimeInMillis()
                        <= fiveDayWeather.getTimestampEnd()
                        && calendar.getTimeInMillis()
                        > fiveDayWeather.getTimestampStart()) {
                    ItemHourlyDB itemHourlyDB = new ItemHourlyDB();
                    itemHourlyDB.setDt(itemHourly.getDt());
                    itemHourlyDB.setFiveDayWeatherId(fiveDayWeatherId);
                    itemHourlyDB.setTemp(itemHourly.getMain().getTemp());
                    itemHourlyDB.setWeatherCode(itemHourly.getWeather().get(0).getId());
                    itemHourlyDBBox.put(itemHourlyDB);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        binding.toolbarLayout.searchView.setMenuItem(item);
        return true;
    }

    public void showCityInfoFragment(CityInfo info) {
        AppUtil.showFragment(new CityInfoFragment(info), getSupportFragmentManager(), true);
    }

    public void showAboutFragment() {
        AppUtil.showFragment(new AboutFragment(), getSupportFragmentManager(), true);
    }

    @Override
    public void onBackPressed() {
        if (binding.toolbarLayout.searchView.isSearchOpen()) {
            binding.toolbarLayout.searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @AfterPermissionGranted(RC_LOCATION_PERM)
    private void requestWeather() {

        if (hasLocationPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    askPermission();
                    return;
                }
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    requestWeatherNow(location);

                                } else {
                                    requestNewLocationData();

                                }
                            }

                        });
            } else {
                Toast.makeText(getApplicationContext(), R.string.enable_location, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // Ask for both permissions
           askPermission();
        }
    }

    private void askPermission() {
        EasyPermissions.requestPermissions(
                this,
                getString(R.string.rationale_location),
                RC_LOCATION_PERM,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askPermission();
            return;
        }
        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private void requestWeatherNow(Location location) {
        Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                requestWeather(addresses.get(0).getLocality(), true);

            } else {
                SnackbarUtil
                        .with(binding.swipeContainer)
                        .setMessage(getString(R.string.no_city_found_message))
                        .setDuration(SnackbarUtil.LENGTH_LONG)
                        .showError();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean hasLocationPermissions() {
        return EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d("Favorite Fragment", "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onRationaleAccepted(int requestCode) {

    }

    @Override
    public void onRationaleDenied(int requestCode) {

    }
}
