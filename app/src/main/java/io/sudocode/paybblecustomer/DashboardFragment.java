package io.sudocode.paybblecustomer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A placeholder fragment containing a simple view.
 */
public class DashboardFragment extends Fragment implements OnMapReadyCallback {

    @Bind(R.id.item_name)
    EditText mItemName;
    @Bind(R.id.item_quantity)
    EditText mItemQuantity;
    @Bind(R.id.additional_requests)
    EditText mAdditionalRequests;
    @Bind(R.id.amount)
    TextView mAmount;
    @Bind(R.id.vat)
    TextView mVat;
    @Bind(R.id.total_amount)
    TextView mTotalAmount;
    @Bind(R.id.place_order_button)
    FloatingActionButton mPlaceOrderButton;
    @Bind(R.id.scan_qr_code_button)
    Button mScanCodeButton;
    @Bind(R.id.map_card_view)
    CardView mMapCardView;
    @Bind(R.id.btc_wallet)
    EditText mBtcWallet;

    private SupportMapFragment mMapFragment;
    private GoogleMap mMap;
    private Marker mCustomerMarker;
    private Marker mVendorMarker;

    private double mTotalAmountToPay;

    public DashboardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, v);

        Toolbar toolbar = ButterKnife.findById(v, R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mItemQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int quantity = 0;
                if (!TextUtils.isEmpty(mItemQuantity.getText()))
                    quantity = Integer.parseInt(charSequence.toString());
                double amount = 100.0 * quantity;
                double vat = 0.14 * amount;
                mTotalAmountToPay = amount + vat;
                mAmount.setText("Amount: " + Double.toString(amount));
                mVat.setText(String.format("VAT: %s", Double.toString(vat)));

                try {
                    //JsonObject response = Ion.with(DashboardFragment.this)
                     //       .load("https://api.coinsecureis.cool/v0/noauth/ticker")
                     //       .asJsonObject()
                      //      .get();
                    int rate = 1610000;
                    rate /= 100;
                    mTotalAmount.setText("You have to pay: Rs." + Double.toString(mTotalAmountToPay) +
                        " / 0.007 BTC");
                } catch (Exception e) {}
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mMapFragment = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map));
        mMap = mMapFragment.getMap();

        return v;
    }

    private void initZxingIntent() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setCaptureActivity(CaptureActivityPortrait.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan the delivery QR Code");
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            Toast.makeText(getActivity(), "Payment successful", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.place_order_button)
    public void onPlaceOrderButtonClicked() {
        sendDataToFirebase();
    }

    private void sendDataToFirebase() {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Placing your order ...");
        progressDialog.show();

        Firebase firebase = new Firebase("https://paybble.firebaseio.com/orders");
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                progressDialog.hide();
                // temporary | wrong!!
                mScanCodeButton.setVisibility(View.VISIBLE);
                mPlaceOrderButton.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                progressDialog.hide();
                Toast.makeText(getActivity(), "There seems to be a problem in placing your order.",
                        Toast.LENGTH_LONG).show();
            }
        });

        CustomerOrder customerOrder = new CustomerOrder(new Random().nextInt(6));
        customerOrder.setCustomerName("Mohammed Nafees");
        customerOrder.setItemName(mItemName.getText().toString());
        customerOrder.setQuantity(Integer.parseInt(mItemQuantity.getText().toString()));
        customerOrder.setAmount(mTotalAmountToPay);
        customerOrder.setBtcWallet(mBtcWallet.getText().toString());
        customerOrder.setAdditionalRequests(mAdditionalRequests.getText().toString());
        CustomerOrder.Location location = new CustomerOrder.Location();
        Location customerLocation = ((MainActivity) getActivity()).currentLocation();
        location.setLongitude(customerLocation.getLongitude());
        location.setLatitude(customerLocation.getLatitude());
        customerOrder.setLocation(location);

        firebase = firebase.push();
        //updateGoogleMap(firebase);
        firebase.setValue(customerOrder);
        customerOrder.setKey(firebase.getKey());
        LastPlacedOrder.instance().setLastOrder(customerOrder);

        ((MainActivity) getActivity()).startLocationUpdates();
    }

    private void updateGoogleMap(Firebase firebase) {
        if (!mMapCardView.isShown()) {
            mMapCardView.setVisibility(View.VISIBLE);
        }
        firebase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue().equals("vendor_location")) {
                    double vendor_latitude = (double)dataSnapshot.child("vendor_location")
                            .child("latitude").getValue();
                    double vendor_longitude = (double)dataSnapshot.child("vendor_location")
                            .child("longitude").getValue();

                    if (mMap != null) {
                        Location customerLoc = ((MainActivity) getActivity()).currentLocation();
                        LatLng customer = new LatLng(customerLoc.getLatitude(), customerLoc.getLongitude());
                        if (mCustomerMarker == null) {
                            mCustomerMarker = mMap.addMarker(new MarkerOptions().position(customer)
                                    .title("Customer"));
                        } else {
                            mCustomerMarker.setPosition(customer);
                        }

                        LatLng vendor = new LatLng(vendor_latitude, vendor_longitude);
                        if (mVendorMarker == null) {
                            mVendorMarker = mMap.addMarker(new MarkerOptions().position(vendor).title("Vendor"));
                        } else {
                            mVendorMarker.setPosition(vendor);
                        }

                        LatLngBounds latLngBounds = new LatLngBounds(customer, vendor);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 10));
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    @OnClick(R.id.scan_qr_code_button)
    public void onScanCodeButtonClicked() {
        initZxingIntent();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
    }

}
