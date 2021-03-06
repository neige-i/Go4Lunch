package com.neige_i.go4lunch.view.auth;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.neige_i.go4lunch.view.util.SingleLiveEvent;

import static com.neige_i.go4lunch.view.auth.AuthActivity.GOOGLE_SIGN_IN_REQUEST_CODE;

public class AuthViewModel extends ViewModel {

    @NonNull
    private final SingleLiveEvent<Void> startHomeActivityEvent = new SingleLiveEvent<>();
    @NonNull
    private final SingleLiveEvent<LoginManager> facebookLoginEvent = new SingleLiveEvent<>();

    @NonNull
    private final FirebaseAuth firebaseAuth;
    @NonNull
    private final CallbackManager callbackManager;
    @NonNull
    private final LoginManager loginManager;

    public AuthViewModel() {
        firebaseAuth = FirebaseAuth.getInstance();

        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();
        loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                final AccessToken accessToken = loginResult.getAccessToken();
                firebaseAuthWithCredential(FacebookAuthProvider.getCredential(accessToken.getToken()));
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
                Log.d("Neige", "AuthViewModel::onError: Facebook sign in failed", error);
            }
        });
    }

    @NonNull
    public LiveData<Void> getStartHomeActivityEvent() {
        return startHomeActivityEvent;
    }

    @NonNull
    public LiveData<LoginManager> getFacebookLoginEvent() {
        return facebookLoginEvent;
    }

    public void onFacebookLoggedIn() {
        facebookLoginEvent.setValue(loginManager);
    }

    public void onSignInResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            try {
                final String idToken = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException.class)
                    .getIdToken();
                firebaseAuthWithCredential(GoogleAuthProvider.getCredential(idToken, null));
            } catch (ApiException e) {
                Log.d("Neige", "AuthViewModel::onSignInResult: Google sign in failed", e);
            }
        } else if (requestCode == CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void firebaseAuthWithCredential(@NonNull AuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startHomeActivityEvent.call();
                } else {
                    Log.d("Neige", "AuthViewModel::onComplete: signInWithCredential:failure", task.getException());
                }
            });
    }
}