package hcmus.student.locationmap.map.utilities.place;

import android.os.AsyncTask;

import java.util.List;

import hcmus.student.locationmap.map.utilities.JSONParser;
import hcmus.student.locationmap.model.Place;
import hcmus.student.locationmap.utilities.FetchUrlTask;

public class GetPlaces extends AsyncTask<String, String, List<Place>> {
    PlaceRespondCallback delegate;
    String url;

    public GetPlaces(PlaceRespondCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    protected List<Place> doInBackground(String... params) {
        try {
            url = params[0];
            FetchUrlTask downloadUrl = new FetchUrlTask();
            String googlePlacesData = downloadUrl.fetch(url);
            JSONParser parser = new JSONParser();
            return parser.parsePlace(googlePlacesData);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<Place> result) {
        delegate.onRespond(url, result);
    }
}
