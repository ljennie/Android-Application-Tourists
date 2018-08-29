package com.jennie.eventreporter;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
//用自己的fragment，并把mapView加进来 原因1为了保留导航栏
public class EventMapFragment extends Fragment
        implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mGoogleMap;

    private MapView mMapView;
    private View mView;
    private DatabaseReference database;//去firebase去取数据
    private List<Event> events;//所以event存到event fragment里
    private Marker lastClicked;

    public EventMapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_event_map, container,
                false);
        database = FirebaseDatabase.getInstance().getReference();//初始化
        events = new ArrayList<Event>();
        return mView;

    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMapView = (MapView) mView.findViewById(R.id.event_map_view);
        if (mMapView != null) {
            mMapView.onCreate(null);
            mMapView.onResume();// needed to get the map to display immediately
            mMapView.getMapAsync(this);
            // 对当前mapView进行更新 //getMapAsync调用onMapReady这个方法
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        /*
        MapsInitializer.initialize(getContext());
        double latitude = 17.385044;
        double longitude = 78.486671;

        // Create marker on google map
        MarkerOptions marker = new MarkerOptions().position(
                new LatLng(latitude, longitude)).title("This is your focus");

        // Change marker Icon on google map //显示水滴的图标
        marker.icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_ROSE));

        // Add marker to google map
        googleMap.addMarker(marker);

        // Set up camera configuration, set camera to latitude = 17.385044, longitude = 78.486671, and set Zoom to 12
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)).zoom(12).build();
        // zoom(12)：显示范围

        // Animate the zoom process
        googleMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition)); */
        MapsInitializer.initialize(getContext());
        mGoogleMap = googleMap;
        mGoogleMap.setOnInfoWindowClickListener(this);//给marker加上了click listener
        mGoogleMap.setOnMarkerClickListener(this);

        final LocationTracker locationTracker = new LocationTracker(getActivity());//得到当前位置
        locationTracker.getLocation();

        double curLatitude = locationTracker.getLatitude();//得到经度纬度
        double curLongitude = locationTracker.getLongitude();

        //创建camera的position，定位camera的中心就是我当前的位置
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(curLatitude, curLongitude)).zoom(12).build();
        //将camera定义的中心定位到我当前的位置
        googleMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));
        setUpMarkersCloseToCurLocation(googleMap, curLatitude, curLongitude);
        //再将周围events显示到我们周围


    }


    private void setUpMarkersCloseToCurLocation(final GoogleMap googleMap,
                                                final double curLatitude,
                                                final double curLongitude) {
        events.clear();
        database.child("events").addListenerForSingleValueEvent(new ValueEventListener() {
            //对于events的表进行搜索
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get all available events
                for (DataSnapshot noteDataSnapshot : dataSnapshot.getChildren()) {
                    Event event = noteDataSnapshot.getValue(Event.class);//对他进行遍历，把每个entry转换成event
                    double destLatitude = event.getLatitude();
                    double destLongitude = event.getLongitude();
                    int distance = Utils.distanceBetweenTwoLocations(curLatitude, curLongitude,
                            destLatitude, destLongitude);//得到你当前的位置和event所对应的距离
                    if (distance <= 10) {
                        events.add(event);//小于10mile，event加到class里去
                    }
                }

                // Set up every events
                for (Event event : events) {//对于每个所创建的event，我们需要建立marker
                    // create marker
                    MarkerOptions marker = new MarkerOptions().position(
                            new LatLng(event.getLatitude(), event.getLongitude())).
                            title(event.getTitle());

                    // Changing marker icon
                    marker.icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_ROSE));

                    // adding marker
                    Marker mker = googleMap.addMarker(marker);
                    mker.setTag(event); //marker 和events联系起来，因为之后点击的时候我们需要找到对应的event
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //TODO: do something
            }
        });
    }


    @Override
    //onInfoWindowClick 这个是Google map自带的，点击水滴就能出现相关的信息
    //点击marker的时候会触发这个call back，这个方法就会将我们点击的marker传进来
    public void onInfoWindowClick(Marker marker) {
        Event event = (Event)marker.getTag();//从marker中得到对应的events
        Intent intent = new Intent(getContext(), CommentActivity.class);
        String eventId = event.getId();//得到event的ID，将它加入显现的intent里
        intent.putExtra("EventID", eventId);
        getContext().startActivity(intent);
    }


    @Override
    public boolean onMarkerClick(final Marker marker) {
        final Event event = (Event)marker.getTag();
        if (lastClicked != null && lastClicked.equals(marker)) {
            lastClicked = null;
            marker.hideInfoWindow();
            marker.setIcon(null);
            return true;
        } else {
            lastClicked = marker;
            new AsyncTask<Void, Void, Bitmap>(){
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    Bitmap bitmap = Utils.getBitmapFromURL(event.getImgUri());
                    return bitmap;
                }

                @Override
                protected void onPostExecute(Bitmap  bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                        marker.setTitle(event.getTitle());
                    }
                }
            }.execute();
            return false;
        }
    }





}
