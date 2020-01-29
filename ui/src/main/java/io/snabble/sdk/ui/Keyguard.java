package io.snabble.sdk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.concurrent.Executors;

public class Keyguard {
    private static final int REQUEST_CODE_AUTHENTICATION = 4613;

    private static Callback currentCallback;

    public interface Callback {
        void success();
        void error();
    }

    @SuppressLint({"WrongConstant", "NewApi"})
    public static void unlock(FragmentActivity activity, Callback callback) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            Keyguard.currentCallback = callback;

            boolean supportsBiometricPrompt = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    BiometricManager biometricManager = (BiometricManager) activity.getSystemService(Context.BIOMETRIC_SERVICE);
                    if (biometricManager != null) {
                        supportsBiometricPrompt = biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
                    }
                } else {
                    FingerprintManager fingerprintManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
                    if (fingerprintManager != null) {
                        supportsBiometricPrompt = fingerprintManager.hasEnrolledFingerprints();
                    }
                }
            }

            if (supportsBiometricPrompt) {
                new BiometricPrompt.Builder(activity)
                        .setTitle(activity.getString(R.string.Snabble_Keyguard_title))
                        .setDescription(activity.getString(R.string.Snabble_Keyguard_message))
                        .setNegativeButton(activity.getString(R.string.Snabble_Cancel), Executors.newSingleThreadExecutor(), (dialogInterface, i) -> {
                            error();
                        })
                        .build()
                        .authenticate(new CancellationSignal(), Executors.newSingleThreadExecutor(),
                                new BiometricPrompt.AuthenticationCallback() {
                                    @Override
                                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                                        error();
                                    }

                                    @Override
                                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                                        error();
                                    }

                                    @Override
                                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                                        success();
                                    }

                                    @Override
                                    public void onAuthenticationFailed() {
                                        error();
                                    }
                                });
            } else {
                KeyguardManager km = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);

                if (km != null && km.isKeyguardSecure()) {
                    final FragmentManager fm = activity.getSupportFragmentManager();

                    ActivityResultFragment fragment = new ActivityResultFragment();
                    fm.beginTransaction()
                            .add(fragment, "ActivityResultFragment")
                            .commit();
                    fm.executePendingTransactions();

                    Intent authIntent = km.createConfirmDeviceCredentialIntent(
                            activity.getString(R.string.Snabble_Keyguard_title),
                            activity.getString(R.string.Snabble_Keyguard_message));

                    fragment.startActivityForResult(authIntent, REQUEST_CODE_AUTHENTICATION);
                } else {
                    success();
                }
            }
        });
    }

    private static void success() {
        if (currentCallback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                currentCallback.success();
                currentCallback = null;
            });
        }
    }

    private static void error() {
        if (currentCallback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                currentCallback.error();
                currentCallback = null;
            });
        }
    }

    public static final class ActivityResultFragment extends androidx.fragment.app.Fragment {
        public ActivityResultFragment() {
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            requireFragmentManager().beginTransaction().remove(this).commit();

            if (requestCode == REQUEST_CODE_AUTHENTICATION) {
                if (currentCallback != null) {
                    if (resultCode == Activity.RESULT_OK) {
                        success();
                    } else {
                        error();
                    }
                }
            }
        }
    }
}
