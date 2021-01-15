package hcmus.student.locationmap.weather;

import android.os.AsyncTask;

import org.json.JSONException;


import hcmus.student.locationmap.utilities.FetchUrlTask;
import hcmus.student.locationmap.weather.utilities.DetailWeather;
import hcmus.student.locationmap.weather.utilities.DetailWeatherParser;
import hcmus.student.locationmap.weather.utilities.OnWeatherResponse;

public class WeatherAsynTask extends AsyncTask<String, Void, DetailWeather> {
    OnWeatherResponse delegate;
    public WeatherAsynTask(OnWeatherResponse delegate) {
        this.delegate =  delegate;
    }

    @Override
    protected DetailWeather doInBackground(String... strings) {
        FetchUrlTask fetchUrlTask = new FetchUrlTask();
        String data = fetchUrlTask.fetch(strings[0]);
        DetailWeatherParser parser = new DetailWeatherParser();
        try {
            return parser.parse(data);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(DetailWeather detailWeather) {
        delegate.onWeatherResponse(detailWeather);
    }
}
