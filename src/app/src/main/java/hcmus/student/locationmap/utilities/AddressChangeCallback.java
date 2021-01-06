package hcmus.student.locationmap.utilities;

import hcmus.student.locationmap.model.Place;

public interface AddressChangeCallback {
    void onAddressInsert(Place place);
    void onAddressUpdate(Place place);
    void onAddressDelete(int placeId);
}
