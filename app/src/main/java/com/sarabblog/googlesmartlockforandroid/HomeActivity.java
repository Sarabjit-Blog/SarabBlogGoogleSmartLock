package com.sarabblog.googlesmartlockforandroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by sbagg1 on 12/23/2017.
 */

public class HomeActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    boolean isConnected;
    private Button button;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
        setContentView(R.layout.activity_home);
        button = findViewById(R.id.logout);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected) {
                    Auth.CredentialsApi.disableAutoSignIn(mGoogleApiClient);
                    Intent intent = new Intent(view.getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        isConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
