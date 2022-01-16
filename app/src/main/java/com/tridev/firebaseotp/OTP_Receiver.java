package com.tridev.firebaseotp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OTP_Receiver extends BroadcastReceiver {

    private OTPReceiverListener otpReceiverListener;

    public OTP_Receiver(){
    }

    public void OTPInitListener(OTPReceiverListener otpReceiverListener) {
        this.otpReceiverListener = otpReceiverListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Status status = (Status) bundle.get(SmsRetriever.EXTRA_STATUS);
                if (status != null) {
                    switch (status.getStatusCode()) {
                        case CommonStatusCodes
                                .SUCCESS:
                            String message = (String) bundle.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                            if (message != null) {
                                Pattern pattern = Pattern.compile("\\d{6}");
                                Matcher matcher = pattern.matcher(message);
                                if (matcher.find()) {
                                    String myOtp = matcher.group(0);
                                    if (this.otpReceiverListener != null) {
                                        this.otpReceiverListener.onOTPSuccess(myOtp);
                                    }
                                }
                            }
                            break;

                        case CommonStatusCodes.TIMEOUT:
                            if (this.otpReceiverListener != null) {
                                this.otpReceiverListener.onOTPTimeOut();
                            }
                            break;
                    }
                }
            }
        }
    }


    public interface OTPReceiverListener {
        void onOTPSuccess(String otp);

        void onOTPTimeOut();
    }
}
