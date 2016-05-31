package org.edx.mobile.authentication;

import android.support.annotation.NonNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.edx.mobile.exception.AuthException;
import org.edx.mobile.http.HttpResponseStatusException;
import org.edx.mobile.http.RetroHttpException;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.module.prefs.LoginPrefs;
import org.edx.mobile.services.ServiceManager;
import org.edx.mobile.util.Config;

import retrofit.RestAdapter;

@Singleton
public class LoginAPI {

    @NonNull
    private final LoginService loginService;

    @Inject
    Config config;

    @NonNull
    private final LoginPrefs loginPrefs;

    @NonNull
    private final ServiceManager serviceManager;

    @Inject
    public LoginAPI(@NonNull RestAdapter restAdapter, @NonNull LoginPrefs loginPrefs, @NonNull ServiceManager serviceManager) {
        this.loginPrefs = loginPrefs;
        this.serviceManager = serviceManager;
        loginService = restAdapter.create(LoginService.class);
    }

    public AuthResponse getAccessToken(@NonNull String username,
                                       @NonNull String password) throws RetroHttpException {
        String grantType = "password";
        String clientID = config.getOAuthClientId();
        return loginService.getAccessToken(grantType, clientID, username, password);
    }

    @NonNull
    public AuthResponse logIn(String username, String password) throws Exception {
        try {
            final AuthResponse response = getAccessToken(username, password);
            loginPrefs.storeAuthTokenResponse(response, LoginPrefs.AuthBackend.PASSWORD);
            if (!response.isSuccess()) {
                throw new AuthException(response.error);
            }
            response.profile = serviceManager.getProfile();
            return response;
        } catch (HttpResponseStatusException ex) {
            if (ex.getStatusCode() >= 400 && ex.getStatusCode() < 500) {
                throw new AuthException(ex);
            }
            throw ex;
        }
    }
}
