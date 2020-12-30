package hcmus.student.locationmap;

import com.google.android.gms.maps.model.Marker;

import hcmus.student.locationmap.utilities.LocationChangeCallback;

public interface MainCallbacks {
    void registerLocationChange(LocationChangeCallback delegate);

    void backToPreviousFragment();

    void openMarkerInfo(Marker marker);
}
