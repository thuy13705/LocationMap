package hcmus.student.locationmap.map.direction;

import android.content.Context;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;

import hcmus.student.locationmap.MainActivity;
import hcmus.student.locationmap.R;
import hcmus.student.locationmap.map.search.SearchClickCallback;
import hcmus.student.locationmap.map.search.SearchResultAdapter;
import hcmus.student.locationmap.model.Place;

public class DirectionFragment extends Fragment implements DirectionFragmentCallback {
    final static String[] transportModes = {"driving", "transit", "walking"};
    MainActivity activity;
    Context context;
    int notUserTypingChecker;
    Place origin;
    Place dest;
    SearchView svOrigin, svDest;
    TextView txtDuration;
    String mode;
    TabLayout tabLayout;

    public static DirectionFragment newInstance(LatLng origin, LatLng dest) {
        Bundle args = new Bundle();
        args.putParcelable("origin", origin);
        args.putParcelable("dest", dest);
        DirectionFragment fragment = new DirectionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (MainActivity) getActivity();
        context = getContext();
        notUserTypingChecker = 0;
        mode = transportModes[0];
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_direction, null, false);
        svOrigin = view.findViewById(R.id.svOrigin);
        svDest = view.findViewById(R.id.svDest);
        txtDuration = view.findViewById(R.id.txtDuration);
        RecyclerView lvFirstSearchResult = view.findViewById(R.id.lvFirstSearchResult);
        RecyclerView lvSecondSearchResult = view.findViewById(R.id.lvSecondSearchResult);
        tabLayout = view.findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mode = transportModes[tab.getPosition()];
                activity.drawRoute(origin.getLocation(), dest.getLocation(), mode);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        lvFirstSearchResult.setLayoutManager(new LinearLayoutManager(context));
        lvSecondSearchResult.setLayoutManager(new LinearLayoutManager(context));

        tabLayout = view.findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mode = transportModes[tab.getPosition()];
                activity.drawRoute(origin.getLocation(), dest.getLocation(), mode);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        final SearchResultAdapter firstAdapter = new SearchResultAdapter(context, new SearchClickCallback() {
            @Override
            public void onSearchClickCallback(Place place) {
                activity.drawRoute(place.getLocation(), dest.getLocation(), mode);
            }
        });

        final SearchResultAdapter secondAdapter = new SearchResultAdapter(context, new SearchClickCallback() {
            @Override
            public void onSearchClickCallback(Place place) {
                activity.drawRoute(origin.getLocation(), place.getLocation(), mode);
            }
        });

        ImageButton btnLocate1 = view.findViewById(R.id.btnLocate1);
        ImageButton btnLocate2 = view.findViewById(R.id.btnLocate2);
        btnLocate1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.drawRoute(null, dest.getLocation(), mode);
            }
        });

        btnLocate2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.drawRoute(origin.getLocation(), null, mode);
            }
        });

        lvFirstSearchResult.setAdapter(firstAdapter);
        lvSecondSearchResult.setAdapter(secondAdapter);

        Bundle args = getArguments();

        if (args != null) {
            LatLng originLocation = args.getParcelable("origin");
            LatLng destLocation = args.getParcelable("dest");
            Geocoder geocoder = new Geocoder(context);
            try {

                if (originLocation != null)
                    origin = new Place(
                            0, geocoder.getFromLocation(originLocation.latitude, originLocation.longitude, 1).get(0).getAddressLine(0),
                            originLocation, null);
                else
                    origin = new Place(0, activity.getResources().getString(R.string.txtCurrentLocation), null, null);

                if (destLocation != null)
                    dest = new Place(
                            0, geocoder.getFromLocation(destLocation.latitude, destLocation.longitude, 1).get(0).getAddressLine(0),
                            destLocation, null);
                else
                    dest = new Place(0, activity.getResources().getString(R.string.txtCurrentLocation), null, null);

                svOrigin.setQuery(origin.getName(), false);
                svDest.setQuery(dest.getName(), false);
            } catch (Exception e) {
                origin = null;
                dest = null;
            }
        }

        svOrigin.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (notUserTypingChecker == 0)
                    firstAdapter.search(newText);
                else
                    notUserTypingChecker--;
                return false;
            }
        });

        svDest.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (notUserTypingChecker == 0)
                    secondAdapter.search(newText);
                else
                    notUserTypingChecker--;
                return false;
            }
        });

        Button btnSwap = view.findViewById(R.id.btnSwap);
        btnSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Place temp = origin;
                origin = dest;
                dest = temp;

                activity.drawRoute(origin.getLocation(), dest.getLocation(), mode);
            }
        });

        return view;
    }

    @Override
    public void onRouteChange(LatLng origin, LatLng dest) throws IOException {
        Geocoder geocoder = new Geocoder(context);
        String originName;
        if (origin == null) {
            originName = context.getResources().getString(R.string.txtCurrentLocation);
        } else
            originName = geocoder.getFromLocation(origin.latitude, origin.longitude, 1).get(0).getAddressLine(0);

        String destName;
        if (dest == null) {
            destName = context.getResources().getString(R.string.txtCurrentLocation);
        } else
            destName = geocoder.getFromLocation(dest.latitude, dest.longitude, 1).get(0).getAddressLine(0);

        this.origin = new Place(0, originName, origin, null);
        this.dest = new Place(0, destName, dest, null);
        notUserTypingChecker += 2;
        svOrigin.setQuery(originName, false);
        svDest.setQuery(destName, false);
    }

    @Override
    public void onDurationChange(String duration, int color) {
        txtDuration.setText(duration);
        txtDuration.setTextColor(color);
    }
}
