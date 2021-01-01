package hcmus.student.locationmap.map.direction;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;

public interface DirectionFragmentCallback {
    void onRouteChange(LatLng origin, LatLng dest) throws IOException;
    void onDurationChange(String duration, int color);
}
