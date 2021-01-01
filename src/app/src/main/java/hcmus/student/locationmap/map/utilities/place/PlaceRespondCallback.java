package hcmus.student.locationmap.map.utilities.place;

import java.util.List;

import hcmus.student.locationmap.model.Place;

public interface PlaceRespondCallback {
    void onRespond(String url, List<Place> placeList);
}

