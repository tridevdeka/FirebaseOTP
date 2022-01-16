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

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private SmsBroadcastReceiverListener smsBroadcastReceiverListener;


    public void OTPInitListener(SmsBroadcastReceiverListener smsBroadcastReceiverListener) {
        this.smsBroadcastReceiverListener = smsBroadcastReceiverListener;
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
                                    if (this.smsBroadcastReceiverListener != null) {
                                        this.smsBroadcastReceiverListener.onOTPSuccess(myOtp);
                                    }
                                }
                            }
                            break;

                        case CommonStatusCodes.TIMEOUT:
                            if (this.smsBroadcastReceiverListener != null) {
                                this.smsBroadcastReceiverListener.onOTPTimeOut();
                            }
                            break;
                    }
                }
            }
        }
    }


    public interface SmsBroadcastReceiverListener {
        void onOTPSuccess(String otp);

        void onOTPTimeOut();
    }
}
