package com.maktab.mahdi.locatr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

public class LocatrFragment extends SupportMapFragment {

    private static final int REQUEST_LOCATION_PERMISSIONS = 0;
    private GoogleApiClient mClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private Bitmap mMapImage;
    private GalleryItem mMapItem;
    private Location mCurrentLocation;
    private GoogleMap mMap;


    public static LocatrFragment newInstance() {

        Bundle args = new Bundle();

        LocatrFragment fragment = new LocatrFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                updateUI();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_locatr, menu);
        MenuItem menuItem = menu.findItem(R.id.menu);
        menuItem.setEnabled(mClient.isConnected());
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    @SuppressLint("MissingPermission")
    private void findImage() {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(0);
        request.setNumUpdates(1);

        /*LocationServices.FusedLocationApi
                .requestLocationUpdates(mClient, request, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

                    }
                });*/
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations, this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            new searchTask().execute(location);
                        }
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu:
                if (hasLocationPermission()) {
                    findImage();
                } else {
                    requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    private boolean hasLocationPermission() {
        int result = ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case (REQUEST_LOCATION_PERMISSIONS):
                if (hasLocationPermission()) {
                    findImage();
                }

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private class searchTask extends AsyncTask<Location, Void, Void> {

        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;
        private Location mLocation;

        @Override
        protected Void doInBackground(Location... locations) {
            mLocation = locations[0];
            FlickrFetchr flickrFetchr = new FlickrFetchr();
            List<GalleryItem> list = flickrFetchr.searchPhoto(locations[0]);
            if (list == null || list.size() == 0) {
                return null;
            }
            mGalleryItem = list.get(0);
            byte[] bytes = new byte[0];
            try {
                bytes = flickrFetchr.getUrlBytes(mGalleryItem.getUrl());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mCurrentLocation = mLocation;
            mMapItem = mGalleryItem;
            mMapImage = mBitmap;
            updateUI();

        }
    }

    private void updateUI(){
        if(mMap==null||mMapImage==null){
            return;
        }


        LatLng itemPoint=new LatLng(mMapItem.getmLat(),mMapItem.getmLon());
        LatLng myPoint=new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
        LatLngBounds bounds=new LatLngBounds.Builder()
                .include(itemPoint)
                .include(myPoint)
                .build();
        BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(mMapImage);
        MarkerOptions itemMarker = new MarkerOptions()
                .position(itemPoint)
                .icon(itemBitmap);
        MarkerOptions myMarker = new MarkerOptions()
                .position(myPoint);
        mMap.clear();
        mMap.addMarker(itemMarker);
        mMap.addMarker(myMarker);
        int margin=getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        CameraUpdate cameraUpdate=CameraUpdateFactory.newLatLngBounds(bounds,margin);
        mMap.animateCamera(cameraUpdate);
    }
}
