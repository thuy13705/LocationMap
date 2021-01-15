package hcmus.student.locationmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import hcmus.student.locationmap.map.utilities.LocationChangeCallback;
import hcmus.student.locationmap.model.Place;
import hcmus.student.locationmap.utilities.AddressChangeCallback;
import hcmus.student.locationmap.utilities.AddressProvider;

public interface MainCallbacks {
    void registerLocationChange(LocationChangeCallback delegate);

    void backToPreviousFragment();


    void registerAddressChange(AddressChangeCallback delegate);

    void openMarkerInfo(Marker marker);

    void openSearchResultMarker(LatLng latLng);
    void openAddContact(LatLng latLng);


    void locatePlace(LatLng location);

    void editPlaces(Place place);

    AddressProvider getAddressProvider();
}
