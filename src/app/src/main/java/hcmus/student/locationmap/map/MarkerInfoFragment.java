package hcmus.student.locationmap.map;

import android.content.Context;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.Formatter;
import java.util.Locale;

import hcmus.student.locationmap.MainActivity;
import hcmus.student.locationmap.R;
public class MarkerInfoFragment extends Fragment implements View.OnClickListener {
    private MainActivity activity;
    private Context context;
    private LatLng latLng;
    private TextView txtPlaceName;
    Button btnAdd, btnClose, btnDirection;

    public static MarkerInfoFragment newInstance(Marker marker) {
        MarkerInfoFragment fragment = new MarkerInfoFragment();
        Bundle args = new Bundle();
        args.putDouble("lat", marker.getPosition().latitude);
        args.putDouble("lng", marker.getPosition().longitude);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = (MainActivity) getActivity();
        context = getContext();
        Bundle args = getArguments();
        latLng = new LatLng(args.getDouble("lat"), args.getDouble("lng"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_marker_info, container, false);

        txtPlaceName = view.findViewById(R.id.txtPlaceName);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnDirection = view.findViewById(R.id.btnDirection);
        btnClose = view.findViewById(R.id.btnClose);

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        Bundle args = getArguments();
        txtPlaceName.setText(R.string.txt_loading_address_line);

        latLng = new LatLng(args.getDouble("lat"), args.getDouble("lng"));

        txtPlaceName.setText(formatter.format("(%.2f, %.2f)", latLng.latitude, latLng.longitude).toString());

        btnAdd.setOnClickListener(this);
        btnDirection.setOnClickListener(this);
        btnClose.setOnClickListener(this);

        return view;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDirection:
                activity.drawRoute(null, latLng, "driving");
                activity.backToPreviousFragment();
                break;
            case R.id.btnAdd:
                activity.openAddContact(latLng);
                break;
            case R.id.btnClose:
                activity.backToPreviousFragment();
                break;
        }
    }

}

