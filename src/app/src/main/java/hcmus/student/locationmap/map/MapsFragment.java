package hcmus.student.locationmap.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hcmus.student.locationmap.MainActivity;
import hcmus.student.locationmap.R;
import hcmus.student.locationmap.map.custom_view.MapWrapper;
import hcmus.student.locationmap.map.custom_view.OnMapWrapperTouch;
import hcmus.student.locationmap.map.direction.DirectionFragment;
import hcmus.student.locationmap.map.utilities.LocationChangeCallback;
import hcmus.student.locationmap.map.utilities.SpeedMonitor;
import hcmus.student.locationmap.map.utilities.direction.Direction;
import hcmus.student.locationmap.map.utilities.direction.DirectionResponse;
import hcmus.student.locationmap.map.utilities.direction.DirectionTask;
import hcmus.student.locationmap.model.Place;
import hcmus.student.locationmap.utilities.AddressChangeCallback;
import hcmus.student.locationmap.utilities.AddressProvider;
import hcmus.student.locationmap.utilities.Storage;

public class MapsFragment extends Fragment implements OnMapReadyCallback, LocationChangeCallback,
        MapsFragmentCallback, DirectionResponse, AddressChangeCallback {

    private static final int DEFAULT_ZOOM = 15;
    private static final int NORMAL_ROUTE_WIDTH = 8;
    private static final int SELECTED_ROUTE_WIDTH = 12;
    private static final double THRESHOLD = 1e-6;

    private MainActivity main;
    private Context context;

    private GoogleMap mMap;
    private Location mCurrentLocation;
    private Marker mLocationIndicator;
    private AddressProvider mAddressProvider;
    private Marker mDefaultMarker;
    private MapView mMapView;
    private MarkerAnimator animator;
    private boolean isCameraFollowing;
    private boolean isContactShown;
    private List<Marker> mContactMarkers;
    private SpeedMonitor speedMonitor;
    private FloatingActionButton btnLocation;
    private FloatingActionButton btnContact;    private TextView txtSpeed;
    private Handler velocityHandler;
    private Runnable velocityRunnable;
    private DirectionFragment directionFragment;

    private ArrayList<Polyline> mRoutes;
    private Marker mRouteStartMarker, mRouteEndMarker;

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

        mContactMarkers = new ArrayList<>();
        mRouteStartMarker = mRouteEndMarker = null;
        mAddressProvider = main.getAddressProvider();
        isContactShown = false;

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
        main.registerAddressChange((AddressChangeCallback) this);
        mMapView.getMapAsync(this);
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        btnContact = getView().findViewById(R.id.btnContact);
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
                main.openMarkerInfo(marker);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                return true;
            }
        });

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onPolylineClick(Polyline polyline) {
                for (Polyline route : mRoutes) {
                    route.setZIndex(0);
                    route.setWidth(NORMAL_ROUTE_WIDTH);
                }
                polyline.setWidth(SELECTED_ROUTE_WIDTH);
                polyline.setZIndex(1);
                directionFragment.onDurationChange(polyline.getTag().toString(), polyline.getColor());
            }
        });
        btnContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isContactShown) {
                    hideAllAddress();
                    btnContact.clearColorFilter();
                    isContactShown = false;
                } else {
                    showAllAddress();
                    int color = getResources().getColor(R.color.colorPrimary);
                    btnContact.setColorFilter(color);
                    isContactShown = true;
                }
            }
        });
    }
    private void hideAllAddress() {
        for (Marker marker : mContactMarkers) {
            marker.setVisible(false);
        }
    }

    @Override
    public void moveCamera(LatLng location) {
        stopFollowing();
        LatLng markerLoc = new LatLng(location.latitude, location.longitude);
        final CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(markerLoc).zoom(15).tilt(30).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        for (Place place : mAddressProvider.getPlaces()) {
            if (compareLatLng(place.getLocation(), location)) {
                showAllAddress();
                isContactShown = true;
                break;
            }
        }
    }


    private boolean compareLatLng(LatLng latLng1, LatLng latLng2) {
        return Math.abs(latLng1.latitude - latLng2.latitude) < THRESHOLD && Math.abs(latLng1.longitude - latLng2.longitude) < THRESHOLD;
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

    private void showAllAddress() {
        for (Marker marker : mContactMarkers) {
            marker.setVisible(true);
        }
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

    @Override
    public void openSearchResultMarker(LatLng coordinate) {
        if (mDefaultMarker != null)
            mDefaultMarker.remove();
        mDefaultMarker = mMap.addMarker(new MarkerOptions().position(coordinate));
        stopFollowing();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(coordinate));
    }

    public void drawRoute(LatLng start, LatLng end, String mode) {
        stopFollowing();
        LatLng startPos = start == null ? new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()) : start;
        LatLng endPos = end == null ? new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()) : end;
        if (directionFragment == null)
            showDirectionFragment(null, endPos);
        else {
            try {
                directionFragment.onRouteChange(start, end);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String url = Direction.getDirectionUrl(startPos, endPos, mode, main);
        new DirectionTask(this).execute(url);
    }

    @Override
    public void onRouteRespond
            (List<PolylineOptions> polylineOptions, List<String> durations) {
        if (mRoutes != null) {
            for (int i = 0; i < mRoutes.size(); i++) {
                mRoutes.get(i).remove();
            }
        }

        if (polylineOptions == null || polylineOptions.size() == 0) {
            Toast.makeText(context, "Cannot find direction to this location", Toast.LENGTH_SHORT).show();
            return;
        }

        mRoutes = new ArrayList<>();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < polylineOptions.size(); i++) {
            PolylineOptions route = polylineOptions.get(i);
            Polyline polyline = mMap.addPolyline((route));
            polyline.setClickable(true);
            polyline.setTag(durations.get(i));
            polyline.setZIndex(0);
            mRoutes.add(polyline);
            List<LatLng> points = route.getPoints();
            for (LatLng point : points) {
                builder.include(point);
            }
            if (i == polylineOptions.size() - 1) {
                polyline.setWidth(SELECTED_ROUTE_WIDTH);
                directionFragment.onDurationChange(durations.get(i), polyline.getColor());
            }
        }

        LatLngBounds bounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0), 1000, null);

        BitmapDrawable bitmapDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.marker_point,
                context.getTheme());
        Bitmap bmp = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 36, 36, false);
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bmp);
        if (mRouteStartMarker != null)
            mRouteStartMarker.remove();
        if (mRouteEndMarker != null)
            mRouteEndMarker.remove();

        mRouteStartMarker = mMap.addMarker(new MarkerOptions()
                .position(polylineOptions.get(0).getPoints().get(0))
                .icon(descriptor).anchor(0.5f, 0.5f));
        mRouteEndMarker = mMap.addMarker(new MarkerOptions()
                .position(polylineOptions.get(0).getPoints().get(polylineOptions.get(0).getPoints().size() - 1))
                .icon(descriptor).anchor(0.5f, 0.5f));
    }

    public void showDirectionFragment(LatLng origin, LatLng dest) {
        directionFragment = DirectionFragment.newInstance(origin, dest);

        FragmentManager manager = getChildFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.frameTop, directionFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void closeDirection() {
        getChildFragmentManager().popBackStack();

        if (mRoutes != null) {
            for (int i = 0; i < mRoutes.size(); i++) {
                mRoutes.get(i).remove();
            }
        }

        if (mRouteStartMarker != null)
            mRouteStartMarker.remove();
        if (mRouteEndMarker != null)
            mRouteEndMarker.remove();

        directionFragment = null;
        Fragment fm = getFragmentManager().findFragmentById(R.id.frameRouteInfo);
        if (fm != null && fm.isAdded())
            main.getSupportFragmentManager().beginTransaction().remove(fm).commit();
    }

    private Marker createMarker(int id, LatLng location, String avatar) {
        if (mDefaultMarker != null && compareLatLng(mDefaultMarker.getPosition(), location)) {
            mDefaultMarker.remove();
            mDefaultMarker = null;
        }

        Bitmap bmpMarker = BitmapFactory.decodeResource(getResources(), R.drawable.marker_frame).copy(Bitmap.Config.ARGB_8888, true);
        bmpMarker = Bitmap.createScaledBitmap(bmpMarker, 100, 110, false);
        if (avatar != null) {
            Storage storage = new Storage(context);
            Bitmap bmpAvatar = storage.readImageFromInternalStorage(avatar);
            bmpAvatar = Bitmap.createScaledBitmap(bmpAvatar, 90, 90, false);
            Canvas canvas = new Canvas(bmpMarker);
            canvas.drawBitmap(bmpAvatar, 5, 5, null);
        }

        Marker newMarker = mMap.addMarker(new MarkerOptions().position(location)
                .icon(BitmapDescriptorFactory.fromBitmap(bmpMarker)));
        newMarker.setZIndex(3);
        newMarker.setTag(id);
        return newMarker;
    }

    private void updateContactMarkers() {
        mContactMarkers.clear();
        for (Place place : mAddressProvider.getPlaces()) {
            Marker newMarker = createMarker(place.getId(), place.getLocation(), place.getAvatar());
            newMarker.setVisible(isContactShown);
            mContactMarkers.add(newMarker);
        }
    }

    @Override
    public void onAddressInsert(Place place) {
        mContactMarkers.add(createMarker(place.getId(), place.getLocation(), place.getAvatar()));
    }

    @Override
    public void onAddressUpdate(Place place) {
        for (Marker marker : mContactMarkers) {
            marker.remove();
        }
        updateContactMarkers();
    }

    @Override
    public void onAddressDelete(int placeId) {
        for (Marker marker : mContactMarkers) {
            if ((int) marker.getTag() == placeId) {
                marker.remove();
                mContactMarkers.remove(marker);
                break;
            }
        }
    }
}