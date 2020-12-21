package hcmus.student.locationmap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import hcmus.student.locationmap.utilities.LocationService;
import hcmus.student.locationmap.utilities.LocationChangeCallback;
import hcmus.student.locationmap.utilities.ViewPagerAdapter;


public class MainActivity extends FragmentActivity implements MainCallbacks, LocationChangeCallback {
    private static final int LOCATION_STATUS_CODE = 1;
    private ViewPager2 mViewPager;
    private ViewPagerAdapter adapter;
    private Location mCurrentLocation;
    private LocationService service;
    private List<LocationChangeCallback> delegates;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TabLayout mTabs = findViewById(R.id.tabs);

        mViewPager = findViewById(R.id.pager);
        mViewPager.setUserInputEnabled(false);  //Disable swipe

        adapter = new ViewPagerAdapter(this);
        mViewPager.setAdapter(new ViewPagerAdapter(this));
        mViewPager.setAdapter(adapter);

        mTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() > 0 && mCurrentLocation == null) {
                    Toast.makeText(MainActivity.this, R.string.txtDetectingLocation, Toast.LENGTH_SHORT).show();
                    mTabs.selectTab(mTabs.getTabAt(0));
                    return;
                }
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        service = new LocationService(this, this);
        delegates = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocation();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showAlert();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_STATUS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_STATUS_CODE) {
            if (grantResults.length > 0)
                enableLocation();
        }
    }

    private void notifyLocationChange() {
        for (LocationChangeCallback delegate : delegates) {
            delegate.onLocationChange(mCurrentLocation);
        }
    }

    private void enableLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            service.start();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning!");
            builder.setMessage("You have denied the app to access your permission, this app won't work");
            builder.setCancelable(false);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void showAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning!");
        builder.setMessage("App must have permission to access you location");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes, I know", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_STATUS_CODE);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public Location getLocation() {
        return mCurrentLocation;
    }

    @Override
    public void onLocationChange(Location location) {
        mCurrentLocation = location;
        notifyLocationChange();
    }

    @Override
    public void registerLocationChange(LocationChangeCallback delegate) {
        delegates.add(delegate);
    }
}