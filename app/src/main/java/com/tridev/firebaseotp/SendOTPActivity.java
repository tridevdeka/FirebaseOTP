package com.tridev.firebaseotp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import android.widget.Toast;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.CredentialsOptions;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.tridev.firebaseotp.databinding.ActivitySendOtpactivityBinding;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SendOTPActivity extends AppCompatActivity {
    private static final int RC_HINT = 1;
    private static final String TAG = "SendOTPActivity";
    private FirebaseAuth auth;
    private ActivitySendOtpactivityBinding binding;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private InputMethodManager inputManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySendOtpactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        phoneSelection();


        Objects.requireNonNull(getSupportActionBar()).hide();

        inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);


        auth = FirebaseAuth.getInstance();


        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(SendOTPActivity.this, MainActivity.class));
            finish();
        }


        binding.btnGetOtp.setOnClickListener(v -> {
            if (binding.edtMobileNumber.getText().toString().isEmpty()) {
                binding.edtMobileNumber.setError("Empty field");
            } else {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.btnGetOtp.setVisibility(View.GONE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                sendOTP();
                // phoneAuthProvider();

            }
        });


    }


    private void phoneSelection() {
        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(new CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build())
                .setPhoneNumberIdentifierSupported(true)
//                .setEmailAddressIdentifierSupported(true)
//                .setAccountTypes(IdentityProviders.GOOGLE)
                .build();

        CredentialsOptions credentialsOptions = new CredentialsOptions.Builder()
                .forceEnableSaveDialog()
                .build();

        CredentialsClient mCredentialsClient = Credentials.getClient(getApplicationContext(), credentialsOptions);

        PendingIntent intent = mCredentialsClient.getHintPickerIntent(hintRequest);
        try {
            startIntentSenderForResult(intent.getIntentSender(), RC_HINT, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Could not start hint picker Intent", e);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_HINT && resultCode == RESULT_OK) {
            assert data != null;
            Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);

            if (credential.getId().startsWith("+91")) {
                binding.edtMobileNumber.setText(credential.getId().substring(3));
                binding.edtMobileNumber.requestFocus();
            }
        } else if (requestCode == RC_HINT && resultCode == CredentialsApi.ACTIVITY_RESULT_NO_HINTS_AVAILABLE) {
            Toast.makeText(this, "No phone numbers found", Toast.LENGTH_LONG).show();
        }
    }

    private void sendOTP() {
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnGetOtp.setVisibility(View.VISIBLE);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnGetOtp.setVisibility(View.VISIBLE);
                Toast.makeText(SendOTPActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(verificationId, forceResendingToken);

                binding.progressBar.setVisibility(View.GONE);
                binding.btnGetOtp.setVisibility(View.VISIBLE);

                Intent intent = new Intent(getApplicationContext(), VerifyOTP.class);
                intent.putExtra("mobile", binding.edtMobileNumber.getText().toString());
                intent.putExtra("verificationId", verificationId);
                startActivity(intent);

            }
        };


        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber("+91" + binding.edtMobileNumber.getText().toString())       // Phone number to verify
                        .setTimeout(30L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);

    }
}