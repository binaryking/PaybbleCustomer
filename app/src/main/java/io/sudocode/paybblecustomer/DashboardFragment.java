package io.sudocode.paybblecustomer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class DashboardFragment extends Fragment {

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

        v.findViewById(R.id.scan_qr_code_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initZxingIntent();
            }
        });

        v.findViewById(R.id.place_order_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendDataToFirebase();
            }
        });

        mItemQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int quantity = 0;
                if (!TextUtils.isEmpty(mItemQuantity.getText()))
                    quantity = Integer.parseInt(charSequence.toString());
                double amount = 100.0*quantity;
                double vat = 0.14*amount;
                mTotalAmountToPay = amount + vat;
                mAmount.setText("Amount: " + Double.toString(amount));
                mVat.setText(String.format("VAT: %s", Double.toString(vat)));
                mTotalAmount.setText("You have to pay: " + Double.toString(mTotalAmountToPay));
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

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
        }
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
        customerOrder.setAdditionalRequests(mAdditionalRequests.getText().toString());
        CustomerOrder.Location location = new CustomerOrder.Location();
        Location customerLocation = ((MainActivity) getActivity()).currentLocation();
        location.setLongitude(customerLocation.getLongitude());
        location.setLatitude(customerLocation.getLatitude());
        customerOrder.setLocation(location);

        firebase = firebase.push();
        firebase.setValue(customerOrder);
        customerOrder.setKey(firebase.getKey());
        LastPlacedOrder.instance().setLastOrder(customerOrder);

        ((MainActivity) getActivity()).startLocationUpdates();
    }

}
