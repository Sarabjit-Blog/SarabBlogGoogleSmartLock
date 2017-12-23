package com.sarabblog.googlesmartlockforandroid;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "LoginActivity";
    private static final int RC_SAVE = 100;
    private static final int RC_READ = 300;
    private static final String IS_RESOLVING = "is_resolving";
    private static final String IS_REQUESTING = "is_requesting";

    private boolean mIsResolving;
    private boolean mIsRequesting;

    private GoogleApiClient mGoogleApiClient;

    private Button mLoginInButton;
    private ProgressBar mLoginInProgressBar;
    private TextInputLayout mUsernameInputLayout;
    private EditText mEDUsername;
    private TextInputLayout mPasswordInputLayout;
    private EditText mEDPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(IS_RESOLVING);
            mIsRequesting = savedInstanceState.getBoolean(IS_REQUESTING);
        }

        mUsernameInputLayout = (TextInputLayout) findViewById(R.id.usernameInputLayout);
        mPasswordInputLayout = (TextInputLayout) findViewById(R.id.passwordInputLayout);

        mEDUsername = (EditText) findViewById(R.id.edUserName);
        mEDPassword = (EditText) findViewById(R.id.edPassword);

        mLoginInButton = (Button) findViewById(R.id.signInButton);
        mLoginInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                setSignInEnabled(false);
                String username = mUsernameInputLayout.getEditText().getText().toString();
                String password = mPasswordInputLayout.getEditText().getText().toString();

                Credential credential = new Credential.Builder(username)
                        .setPassword(password)
                        .build();
                if (Util.isValidCredential(credential)) {
                    saveCredential(credential);
                } else {
                    Log.d(TAG, "Credentials are invalid. Username or password are " +
                            "incorrect.");
                    Toast.makeText(LoginActivity.this, R.string.invalid_creds_toast_msg,
                            Toast.LENGTH_SHORT).show();
                    setSignInEnabled(true);
                }
            }
        });

        mLoginInProgressBar = (ProgressBar) findViewById(R.id.signInProgress);
        mLoginInProgressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current sign in state
        savedInstanceState.putBoolean(IS_RESOLVING, mIsResolving);
        savedInstanceState.putBoolean(IS_REQUESTING, mIsRequesting);

        // Always call the superclass to save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                processRetrievedCredential(credential);
            } else {
                Log.e(TAG, "Credential Read: NOT OK");
                setSignInEnabled(true);
            }
        } else if (requestCode == RC_SAVE) {
            Log.d(TAG, "Result code: " + resultCode);
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Credential Save: OK");
            } else {
                Log.e(TAG, "Credential Save Failed");
            }
            goToHomePage();
        }
        mIsResolving = false;
    }

    private void requestCredentials() {
        mLoginInButton.setEnabled(false);
        mIsRequesting = true;

        CredentialRequest request = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE)
                .build();

        Auth.CredentialsApi.request(mGoogleApiClient, request).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
                        mIsRequesting = false;
                        Status status = credentialRequestResult.getStatus();
                        if (credentialRequestResult.getStatus().isSuccess()) {
                            /* Successfully read the credential without any user interaction, this
                               means there was only a single credential and the user has auto
                               sign-in enabled.*/
                            Credential credential = credentialRequestResult.getCredential();
                            processRetrievedCredential(credential);
                        } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                            /* This is most likely the case where the user has multiple saved
                               credentials and needs to pick one.*/
                            resolveResult(status, RC_READ);
                        } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                            /* This is most likely the case where the user does not currently
                                have any saved credentials and thus needs to provide a username
                                 and password to sign in.*/
                            mLoginInButton.setEnabled(true);
                        } else {
                            Log.w(TAG, "Unknown statusCode: " + status.getStatusCode());
                            mLoginInButton.setEnabled(true);
                        }
                    }
                }
        );
    }

    private void processRetrievedCredential(Credential credential) {
        String accountType = credential.getAccountType();
        if (accountType == null) {
            if (Util.isValidCredential(credential)) {
                goToHomePage();
            } else {
                /*   This is likely due to the credential being changed outside of
                     Smart Lock, ie: away from Android or Chrome. The credential should be deleted
                     and the user allowed to enter a valid credential.
                */
                Toast.makeText(this, "Retrieved credentials are invalid, so will be deleted.", Toast.LENGTH_LONG).show();
                deleteCredential(credential);
                requestCredentials();
                mLoginInButton.setEnabled(false);
            }
        } else if (accountType.equals(IdentityProviders.GOOGLE)) {
            /* The user has previously signed in with Google Sign-In. Silently
                 sign in the user with the same ID.
                 See https://developers.google.com/identity/sign-in/android/*/
            GoogleSignInOptions gso =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build();
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .setAccountName(credential.getId())
                    .build();
            OptionalPendingResult<GoogleSignInResult> opr =
                    Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
            // ...
        }
    }

    private void resolveResult(Status status, int requestCode) {
        /* We don't want to fire multiple resolutions at once since that can result
           in stacked dialogs after rotation or another similar event.*/
        if (mIsResolving) {
            Log.w(TAG, "resolveResult: in process of resolving.");
            return;
        }

        Log.d(TAG, "Resolving: " + status);
        if (status.hasResolution()) {
            Log.d(TAG, "STATUS: RESOLVING");
            try {
                status.startResolutionForResult(this, requestCode);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "STATUS: Failed to send resolution.", e);
            }
        } else {
            goToHomePage();
        }
    }

    private void saveCredential(final Credential credential) {
        // Credential is valid so save it.
        Auth.CredentialsApi.save(mGoogleApiClient,
                credential).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "Credential saved");
                    goToHomePage();
                } else {
                    Log.d(TAG, "Attempt to save credential failed " +
                            status.getStatusMessage() + " " +
                            status.getStatusCode());
                    resolveResult(status, RC_SAVE);
                }
            }
        });
    }

    private void deleteCredential(Credential credential) {
        Auth.CredentialsApi.delete(mGoogleApiClient,
                credential).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "Credential successfully deleted.");
                } else {
                    /* This may be due to the credential not existing, possibly
                       already deleted via another device/app.*/
                    Log.d(TAG, "Credential not deleted successfully.");
                }
            }
        });
    }

    /**
     * Enable or disable Sign In form.
     *
     * @param enable Enable form when true, disable when false.
     */
    protected void setSignInEnabled(boolean enable) {
        mLoginInButton.setEnabled(enable);
        mEDUsername.setEnabled(enable);
        mEDPassword.setEnabled(enable);
        if (!enable) {
            mLoginInProgressBar.setVisibility(ProgressBar.VISIBLE);
        } else {
            mLoginInProgressBar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    private void goToHomePage() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        /* Request Credentials once connected. If credentials are retrieved
         the user will either be automatically signed in or will be
         presented with credential options to be used by the application
         for sign in.*/
        requestCredentials();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }
}
