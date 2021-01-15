package hcmus.student.locationmap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import hcmus.student.locationmap.address_book.EditPlaceFragment;
import hcmus.student.locationmap.map.AddContactFragment;
import hcmus.student.locationmap.map.MapsFragment;
import hcmus.student.locationmap.map.MarkerInfoFragment;
import hcmus.student.locationmap.map.utilities.LocationChangeCallback;
import hcmus.student.locationmap.model.Place;
import hcmus.student.locationmap.utilities.AddressChangeCallback;
import hcmus.student.locationmap.utilities.AddressProvider;
import hcmus.student.locationmap.utilities.LocationService;
import hcmus.student.locationmap.MainCallbacks;
import hcmus.student.locationmap.utilities.OnAddressChange;
import hcmus.student.locationmap.utilities.OnLocationChange;
import hcmus.student.locationmap.utilities.ViewPagerAdapter;


public class MainActivity extends FragmentActivity implements MainCallbacks, OnLocationChange, AddressChangeCallback {
    private static final int LOCATION_STATUS_CODE = 1;
    private ViewPager2 mViewPager;
    private ViewPagerAdapter adapter;
    private Location mCurrentLocation;
    private LocationService service;
    private List<LocationChangeCallback> delegates;
    private AddressProvider addressProvider;
    private List<AddressChangeCallback> addressDelegates;

    private boolean isGettingLocation = true;
    private View mProgressBar;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = findViewById(R.id.loadingScreen);
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
        addressProvider = new AddressProvider(this, this);
        addressDelegates = new ArrayList<>();

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

    public void backToPreviousFragment() {
        getSupportFragmentManager().popBackStack();
    }

    public void openMarkerInfo(Marker marker) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        fragmentTransaction.replace(R.id.frameBottom, MarkerInfoFragment.newInstance(marker));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.commit();
    }

    public void drawRoute(LatLng start, LatLng end, String mode) {
        MapsFragment fragment = (MapsFragment) adapter.getFragment(0);
        fragment.drawRoute(start, end, mode);
    }

    @Override
    public void locatePlace(LatLng location) {
        mViewPager.setCurrentItem(0);
        MapsFragment fragment = (MapsFragment) adapter.getFragment(0);
        fragment.moveCamera(location);
        TabLayout mTabs = findViewById(R.id.tabs);
        TabLayout.Tab tab = mTabs.getTabAt(0);
        assert tab != null;
        tab.select();
    }

    @Override
    public void editPlaces(Place place) {
        EditPlaceFragment.newInstance(place).show(getSupportFragmentManager(), null);
    }


    @Override
    public void registerLocationChange(LocationChangeCallback delegate) {
        delegates.add(delegate);
    }

    @Override
    public void registerAddressChange(AddressChangeCallback delegate) {
        addressDelegates.add(delegate);
    }

    @Override
    public void openSearchResultMarker(LatLng latLng) {
        MapsFragment fragment = (MapsFragment) adapter.getFragment(0);
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        fragment.openSearchResultMarker(latLng);
    }

    @Override
    public void openAddContact(LatLng latLng) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frameBottom, AddContactFragment.newInstance(latLng));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 0 && adapter.getFragment(0).getChildFragmentManager().getBackStackEntryCount() > 0) {
            ((MapsFragment) adapter.getFragment(0)).closeDirection();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onLocationChange(Location location) {
        if (isGettingLocation) {
            mProgressBar.setVisibility(View.GONE);
            isGettingLocation = false;
        }
        mCurrentLocation = location;
        notifyLocationChange();
    }

    @Override
    public AddressProvider getAddressProvider() {
        return addressProvider;
    }

    @Override
    public void onAddressInsert(Place place) {
        for (AddressChangeCallback delegate : addressDelegates) {
            delegate.onAddressInsert(place);
        }
    }

    @Override
    public void onAddressUpdate(Place place) {
        for (AddressChangeCallback delegate : addressDelegates) {
            delegate.onAddressUpdate(place);
        }
    }

    @Override
    public void onAddressDelete(int placeId) {
        for (AddressChangeCallback delegate : addressDelegates) {
            delegate.onAddressDelete(placeId);
        }
    }
}