package com.tridev.firebaseotp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.tridev.firebaseotp.databinding.ActivityVerifyOtpBinding;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VerifyOTP extends AppCompatActivity {

    private static final String TAG = "VerifyOTPActivity";
    private String verificationId;
    private static final int SMS_CONSENT_REQUEST = 2;  // Set to an unused request code
    private ActivityVerifyOtpBinding binding;
    private FirebaseAuth auth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private OTP_Receiver otp_receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyOtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Objects.requireNonNull(getSupportActionBar()).hide();

        auth = FirebaseAuth.getInstance();

        verificationId = getIntent().getStringExtra("verificationId");


        binding.txtMobile.setText(String.format(
                "+91-%s", getIntent().getStringExtra("mobile")));

        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);


        showTimer();
        setUpOTPInputs();


        binding.txtResend.setOnClickListener(view -> {
            binding.mProgress.setVisibility(View.VISIBLE);
            resendOTP();
        });

        binding.btnVerify.setOnClickListener(view -> {

            if (binding.inputCode1.getText().toString().trim().isEmpty()
                    || binding.inputCode2.getText().toString().trim().isEmpty()
                    || binding.inputCode3.getText().toString().trim().isEmpty()
                    || binding.inputCode4.getText().toString().trim().isEmpty()
                    || binding.inputCode5.getText().toString().trim().isEmpty()
                    || binding.inputCode6.getText().toString().trim().isEmpty()
            ) {
                Toast.makeText(VerifyOTP.this, "Enter valid code", Toast.LENGTH_SHORT).show();
            } else {
                verifyOTP();
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        autoOTPReceiver();

    }


    private void autoOTPReceiver() {
        otp_receiver = new OTP_Receiver();
        this.registerReceiver(otp_receiver, new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION));
        otp_receiver.OTPInitListener(new OTP_Receiver.OTPReceiverListener() {
            @Override
            public void onOTPSuccess(String otp) {
                int o1 = Character.getNumericValue(otp.charAt(0));
                int o2 = Character.getNumericValue(otp.charAt(1));
                int o3 = Character.getNumericValue(otp.charAt(2));
                int o4 = Character.getNumericValue(otp.charAt(3));
                int o5 = Character.getNumericValue(otp.charAt(4));
                int o6 = Character.getNumericValue(otp.charAt(5));

                binding.inputCode1.setText(String.valueOf(o1));
                binding.inputCode2.setText(String.valueOf(o2));
                binding.inputCode3.setText(String.valueOf(o3));
                binding.inputCode4.setText(String.valueOf(o4));
                binding.inputCode5.setText(String.valueOf(o5));
                binding.inputCode6.setText(String.valueOf(o6));
            }

            @Override
            public void onOTPTimeOut() {
                Toast.makeText(VerifyOTP.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (otp_receiver != null) {
            unregisterReceiver(otp_receiver);
        }


        SmsRetrieverClient client = SmsRetriever.getClient(this);
        Task<Void> task = client.startSmsRetriever();
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(VerifyOTP.this, "Successful", Toast.LENGTH_SHORT).show();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(VerifyOTP.this, "Failed to retrieve", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTimer() {
        new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                NumberFormat f = new DecimalFormat("00");
                long hour = (millisUntilFinished / 3600000) % 24;
                long min = (millisUntilFinished / 60000) % 60;
                long sec = (millisUntilFinished / 1000) % 60;


                if (sec == 1) {

                    binding.txtResend.setVisibility(View.VISIBLE);
                    binding.txtDidnt.setVisibility(View.VISIBLE);
                    binding.txtTimer.setTextColor(getResources().getColor(R.color.colorGreen));
                } else {
                    binding.txtTimer.setText(f.format(min) + ":" + f.format(sec));
                }

            }

            @Override
            public void onFinish() {
            }
        }.start();
    }


    private void verifyOTP() {

        String code = binding.inputCode1.getText().toString()
                + binding.inputCode2.getText().toString()
                + binding.inputCode3.getText().toString()
                + binding.inputCode4.getText().toString()
                + binding.inputCode5.getText().toString()
                + binding.inputCode6.getText().toString();

        if (verificationId != null) {
            binding.mProgress.setVisibility(View.VISIBLE);
            binding.btnVerify.setVisibility(View.GONE);

            PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(
                    verificationId,
                    code
            );

            auth.signInWithCredential(phoneAuthCredential)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            binding.mProgress.setVisibility(View.GONE);
                            binding.btnVerify.setVisibility(View.VISIBLE);
                            if (task.isSuccessful()) {
                                Intent intent = new Intent(VerifyOTP.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                Toast.makeText(VerifyOTP.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }


    private void resendOTP() {
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                binding.mProgress.setVisibility(View.GONE);
                Toast.makeText(VerifyOTP.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(@NonNull String newVerificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(newVerificationId, forceResendingToken);

                verificationId = newVerificationId;
                Toast.makeText(VerifyOTP.this, "OTP sent", Toast.LENGTH_SHORT).show();
                binding.mProgress.setVisibility(View.GONE);
                binding.btnVerify.setVisibility(View.VISIBLE);

            }
        };


        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber("+91" + getIntent().getStringExtra("mobile"))      // Phone number to verify
                        .setTimeout(30L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(VerifyOTP.this)                 // Activity (for callback binding)
                        .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);

    }


    private void setUpOTPInputs() {

        binding.inputCode1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty()) {
                    binding.inputCode2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        binding.inputCode2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty()) {
                    binding.inputCode3.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        binding.inputCode3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty()) {
                    binding.inputCode4.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        binding.inputCode4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty()) {
                    binding.inputCode5.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        binding.inputCode5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty()) {
                    binding.inputCode6.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }
}