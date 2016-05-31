package org.edx.mobile.task;

import android.content.Context;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

import org.edx.mobile.authentication.AuthResponse;
import org.edx.mobile.authentication.LoginAPI;
import org.edx.mobile.model.api.RegisterResponse;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.module.prefs.LoginPrefs;
import org.edx.mobile.module.prefs.PrefManager;
import org.edx.mobile.services.ServiceManager;
import org.edx.mobile.social.SocialFactory;


public abstract class RegisterTask extends Task<RegisterResponse> {

    private Bundle parameters;
    private AuthResponse auth;
    private SocialFactory.SOCIAL_SOURCE_TYPE backstoreType;
    private String accessToken;

    public RegisterTask(Context context, Bundle parameters, String accessToken, SocialFactory.SOCIAL_SOURCE_TYPE backstoreType) {
        super(context);
        this.parameters = parameters;
        this.accessToken = accessToken;
        this.backstoreType = backstoreType;
    }

    @Inject
    LoginAPI loginAPI;

    @Inject
    LoginPrefs loginPrefs;

    @Override
    public RegisterResponse call() throws Exception {
        ServiceManager api = environment.getServiceManager();
        RegisterResponse res = api.register(parameters);

        if (!res.isSuccess()) {
            return res;
        }

        switch (backstoreType) {
            case TYPE_GOOGLE:
                auth = api.loginByGoogle(accessToken);
                break;
            case TYPE_FACEBOOK:
                auth = api.loginByFacebook(accessToken);
                break;
            default: // normal email address login
                String username = parameters.getString("username");
                String password = parameters.getString("password");

                auth = loginAPI.getAccessToken(username, password);

                loginPrefs.storeAuthTokenResponse(auth, LoginPrefs.AuthBackend.PASSWORD);

                if (auth.isSuccess()) {
                    logger.debug("login succeeded after email registration");
                }
        }
        if (auth != null && auth.isSuccess()) {
            auth.profile = api.getProfile();
        }
        return res;
    }

    public AuthResponse getAuth() {
        return auth;
    }
}
