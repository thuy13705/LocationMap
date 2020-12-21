package hcmus.student.locationmap;

import hcmus.student.locationmap.utilities.LocationChangeCallback;

public interface MainCallbacks {
    void registerLocationChange(LocationChangeCallback delegate);
}
