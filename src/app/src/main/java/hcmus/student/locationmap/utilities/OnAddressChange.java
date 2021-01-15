package hcmus.student.locationmap.utilities;

import hcmus.student.locationmap.model.Place;

public interface OnAddressChange {
    void onAddressInsert(Place place);
    void onAddressUpdate(Place place);
    void onAddressDelete(int placeId);
}