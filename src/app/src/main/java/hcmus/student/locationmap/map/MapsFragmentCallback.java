package hcmus.student.locationmap.map;

import com.google.android.gms.maps.model.LatLng;

public interface MapsFragmentCallback {
    void openSearchResultMarker(LatLng latLng);
}
