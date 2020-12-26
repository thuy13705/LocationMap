package hcmus.student.locationmap.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

import hcmus.student.locationmap.MainActivity;
import hcmus.student.locationmap.R;
import hcmus.student.locationmap.map.custom_view.MapWrapper;
import hcmus.student.locationmap.map.custom_view.OnMapWrapperTouch;
import hcmus.student.locationmap.map.utilities.SpeedMonitor;
import hcmus.student.locationmap.utilities.LocationChangeCallback;

public class MapsFragment extends Fragment implements OnMapReadyCallback, LocationChangeCallback {

    private static final int DEFAULT_ZOOM = 15;
    private static final int NORMAL_ROUTE_WIDTH = 8;
    private static final int SELECTED_ROUTE_WIDTH = 12;

    private MainActivity main;
    private Context context;

    private GoogleMap mMap;
    private Location mCurrentLocation;
    private Marker mLocationIndicator;
    private Marker mDefaultMarker;
    private MapView mMapView;
    private MarkerAnimator animator;
    private boolean isCameraFollowing;
    private boolean isContactShown;
    private SpeedMonitor speedMonitor;
    private FloatingActionButton btnLocation;
    private TextView txtSpeed;
    private Handler velocityHandler;
    private Runnable velocityRunnable;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static MapsFragment newInstance() {
        MapsFragment fragment = new MapsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        main = (MainActivity) getActivity();
        isCameraFollowing = true;

        velocityHandler = new Handler();
        velocityRunnable = null;
        speedMonitor = new SpeedMonitor(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        mMapView = view.findViewById(R.id.map);
        txtSpeed = view.findViewById(R.id.txtSpeed);

        mMapView.onCreate(savedInstanceState);
        main.registerLocationChange(this);
        mMapView.getMapAsync(this);
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        btnLocation = getView().findViewById(R.id.btnLocation);
        final MapWrapper mapContainer = getView().findViewById(R.id.mapContainer);

        mapContainer.setOnMapWrapperTouch(new OnMapWrapperTouch() {
            @Override
            public void onMapWrapperTouch() {
                if (isCameraFollowing) {
                    isCameraFollowing = false;
                    btnLocation.clearColorFilter();
                }
            }
        });

        btnLocation.setOnClickListener(new View.OnClickListener() {
            private int clickCount = 0;
            Handler handler = null;

            @Override
            public void onClick(View v) {
                clickCount++;
                if (handler != null)
                    return;
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentLocation == null) {
                            Toast.makeText(context, R.string.txtNullLocation, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final boolean check = clickCount >= 2;

                        float zoomLevel = mMap.getCameraPosition().zoom < DEFAULT_ZOOM ? DEFAULT_ZOOM : mMap.getCameraPosition().zoom;

                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(),
                                mCurrentLocation.getLongitude()), zoomLevel);
                        mMap.animateCamera(cameraUpdate, new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                if (check && !isCameraFollowing) {
                                    isCameraFollowing = true;
                                    int color = getResources().getColor(R.color.colorPrimary);
                                    btnLocation.setColorFilter(color);
                                }
                            }

                            @Override
                            public void onCancel() {
                                if (isCameraFollowing) {
                                    isCameraFollowing = false;
                                    btnLocation.clearColorFilter();
                                }
                            }
                        });

                        clickCount = 0;
                        handler = null;
                    }
                }, 500);
            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (mDefaultMarker != null) mDefaultMarker.remove();
                stopFollowing();
                mDefaultMarker = mMap.addMarker(new MarkerOptions().position(latLng));
                mDefaultMarker.setZIndex(5);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.getPosition().equals(mLocationIndicator.getPosition()))
                    return true;
                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                return true;
            }
        });
    }

    @Override
    public void onLocationChange(Location location) {
        if (mMap == null)
            return;

        //Display location indicator
        if (mCurrentLocation == null) {
            int color = getResources().getColor(R.color.colorPrimary);
            btnLocation.setColorFilter(color);

            BitmapDrawable bitmapDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.location_indicator,
                    context.getTheme());
            Bitmap bitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 72, 72, false);
            bitmapDrawable.setAntiAlias(true);
            mLocationIndicator = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).flat(true)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap)).anchor(0.5f, 0.5f));

            mLocationIndicator.setZIndex(3);

            animator = new MarkerAnimator(mLocationIndicator, mMap);        //Move camera to user location with default zoom
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                    location.getLongitude()), DEFAULT_ZOOM));
        }
        mCurrentLocation = location;
        double speed = speedMonitor.getSpeed(mCurrentLocation);
        if (speed >= 0)
            txtSpeed.setText(String.format(Locale.US, "%.1f", speed));

        if (velocityRunnable != null) {
            velocityHandler.removeCallbacks(velocityRunnable);
        }

        velocityRunnable = new Runnable() {
            @Override
            public void run() {
                txtSpeed.setText("0");
            }
        };

        velocityHandler.postDelayed(velocityRunnable, 5000);
        animator.animate(location, isCameraFollowing);
    }

    public void stopFollowing() {
        isCameraFollowing = false;
        btnLocation.clearColorFilter();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }
}