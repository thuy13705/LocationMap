package hcmus.student.locationmap.weather.utilities;

import android.os.AsyncTask;

import org.json.JSONException;

import hcmus.student.locationmap.utilities.FetchUrlTask;

public class GetWeatherDetailTask extends AsyncTask<String, Void, DetailWeather> {
    OnDetailWeatherResponse delegate;

    public GetWeatherDetailTask(OnDetailWeatherResponse delegate) {
        this.delegate = delegate;
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
        delegate.onDetailWeatherResponse(detailWeather);
    }
}
