package com.dvt.weatherapp.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.dvt.weatherapp.R;
import com.dvt.weatherapp.databinding.FragmentMultipleDaysBinding;
import com.dvt.weatherapp.model.db.FavoriteWeather;
import com.dvt.weatherapp.ui.activity.MapActivity;
import com.dvt.weatherapp.utils.DbUtil;
import com.dvt.weatherapp.utils.MyApplication;
import com.github.pwittchen.prefser.library.rx2.Prefser;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidScheduler;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.reactivex.disposables.CompositeDisposable;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class FavoriteFragment extends DialogFragment implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {
    private static final int RC_LOCATION_PERM = 123;
    private final String defaultLang = "en";
    private final CompositeDisposable disposable = new CompositeDisposable();
    private FastAdapter<FavoriteWeather> mFastAdapter;
    private ItemAdapter<FavoriteWeather> mItemAdapter;
    private Activity activity;
    private Box<FavoriteWeather> favoriteWeatherBox;
    private Prefser prefser;
    private String apiKey;
    private FragmentMultipleDaysBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMultipleDaysBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        initVariables();
        initSwipeView();
        initRecyclerView();
        showStoredFavoriteWeather();
        //checkTimePass();
        return view;
    }

    private void initVariables() {
        activity = getActivity();
        prefser = new Prefser(activity);
        BoxStore boxStore = MyApplication.getBoxStore();
        favoriteWeatherBox = boxStore.boxFor(FavoriteWeather.class);
        binding.closeButton.setOnClickListener(v -> {
            dismiss();
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });
        binding.showMapButton.setOnClickListener(v -> {
            startMapActivity();
        });
    }

    @AfterPermissionGranted(RC_LOCATION_PERM)
    private void startMapActivity() {
        if (hasLocationPermissions()) {
            Intent intent = new Intent(activity, MapActivity.class);
            activity.startActivity(intent);
        } else {
            // Ask for both permissions
            EasyPermissions.requestPermissions(
                    activity,
                    getString(R.string.rationale_location),
                    RC_LOCATION_PERM,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private boolean hasLocationPermissions() {
        return EasyPermissions.hasPermissions(activity, Manifest.permission.ACCESS_FINE_LOCATION);
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

    private void initSwipeView() {
        binding.swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
            }

        });
    }


    private void initRecyclerView() {
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        binding.recyclerView.setLayoutManager(layoutManager);
        mItemAdapter = new ItemAdapter<>();
        mFastAdapter = FastAdapter.with(mItemAdapter);
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerView.setAdapter(mFastAdapter);
    }


    private void showStoredFavoriteWeather() {
        Query<FavoriteWeather> query = DbUtil.getFavoriteWeatherQuery(favoriteWeatherBox);
        query.subscribe().on(AndroidScheduler.mainThread())
                .observer(new DataObserver<List<FavoriteWeather>>() {
                    @Override
                    public void onData(@NonNull List<FavoriteWeather> data) {
                        if (data.size() > 0) {
                            hideEmptyLayout();
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // data.remove(0);
                                    mItemAdapter.clear();
                                    mItemAdapter.add(data);
                                }
                            }, 500);
                        } else {
                            showEmptyLayout();
                        }
                    }
                });
    }

    private void showEmptyLayout() {
        Glide.with(activity).load(R.drawable.no_city).into(binding.contentEmptyLayout.noCityImageView);
        binding.contentEmptyLayout.emptyLayout.setVisibility(View.VISIBLE);

    }

    private void hideEmptyLayout() {
        binding.contentEmptyLayout.emptyLayout.setVisibility(View.GONE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    @Override
    public void onRationaleAccepted(int requestCode) {

    }

    @Override
    public void onRationaleDenied(int requestCode) {

    }
}
