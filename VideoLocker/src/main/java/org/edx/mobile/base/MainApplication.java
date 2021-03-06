package org.edx.mobile.base;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.newrelic.agent.android.NewRelic;

import org.edx.mobile.BuildConfig;
import org.edx.mobile.R;
import org.edx.mobile.core.EdxDefaultModule;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.module.notification.NotificationDelegate;
import org.edx.mobile.module.prefs.PrefManager;
import org.edx.mobile.module.storage.IStorage;
import org.edx.mobile.util.Config;
import org.edx.mobile.util.NetworkUtil;
import org.edx.mobile.view.Router;

import java.util.Locale;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import io.fabric.sdk.android.Fabric;
import roboguice.RoboGuice;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * This class initializes the modules of the app based on the configuration.
 */
public abstract class MainApplication extends MultiDexApplication {

    //FIXME - temporary solution
    public static final boolean RETROFIT_ENABLED = false;

    protected final Logger logger = new Logger(getClass().getName());

    public static MainApplication application;

    public static final MainApplication instance() {
        return application;
    }

    private Injector injector;

    @Inject
    protected Config config;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    /**
     * Initializes the request manager, image cache,
     * all third party integrations and shared components.
     */
    private void init() {
        application = this;
        registerActivityLifecycleCallbacks(new MyActivityLifecycleCallbacks());

        injector = RoboGuice.getOrCreateBaseApplicationInjector((Application) this, RoboGuice.DEFAULT_STAGE,
                (Module) RoboGuice.newDefaultRoboModule(this), (Module) new EdxDefaultModule(this));

        injector.injectMembers(this);

        // initialize Fabric
        if (config.getFabricConfig().isEnabled() && !BuildConfig.DEBUG) {
            Fabric.with(this, new CrashlyticsCore(), new Answers());
            EventBus.getDefault().register(new CrashlyticsCrashReportObserver());
        }

        // initialize NewRelic with crash reporting disabled
        if (config.getNewRelicConfig().isEnabled()) {
            //Crash reporting for new relic has been disabled
            NewRelic.withApplicationToken(config.getNewRelicConfig().getNewRelicKey())
                    .withCrashReportingEnabled(false)
                    .start(this);
        }

        // initialize Facebook SDK
        boolean isOnZeroRatedNetwork = NetworkUtil.isOnZeroRatedNetwork(getApplicationContext(), config);
        if (!isOnZeroRatedNetwork
                && config.getFacebookConfig().isEnabled()) {
            com.facebook.Settings.setApplicationId(config.getFacebookConfig().getFacebookAppId());
        }

        if (needVersionUpgrade(this)) {
            // try repair of download data if app version is updated
            injector.getInstance(IStorage.class).repairDownloadCompletionData();
        }

        // Register Font Awesome module in android-iconify library
        Iconify.with(new FontAwesomeModule());

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/OpenSans-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
    }

    /**
     * callback when application is launched from background or from a cold launch,
     */
    public void onApplicationLaunchedFromBackground() {
        logger.debug("onApplicationLaunchedFromBackground");
        PrefManager pref = new PrefManager(this, PrefManager.Pref.LOGIN);
        if (pref.hasAuthTokenSocialCookie()) {
            injector.getInstance(Router.class).forceLogout(this, injector.getInstance(ISegment.class), injector.getInstance(NotificationDelegate.class));
        }
    }

    private boolean needVersionUpgrade(Context context) {
        boolean needVersionUpgrade = false;
        PrefManager.AppInfoPrefManager pmanager = new PrefManager.AppInfoPrefManager(context);
        Long previousVersion = pmanager.getAppVersionCode();
        final int curVersion = BuildConfig.VERSION_CODE;
        if (previousVersion < curVersion) {
            needVersionUpgrade = true;
            pmanager.setAppVersionCode(curVersion);
        }
        return needVersionUpgrade;
    }

    public static class CrashlyticsCrashReportObserver {
        @SuppressWarnings("unused")
        public void onEventMainThread(Logger.CrashReportEvent e) {
            CrashlyticsCore.getInstance().logException(e.getError());
        }
    }

    public Injector getInjector() {
        return injector;
    }

    private final class MyActivityLifecycleCallbacks
            implements Application.ActivityLifecycleCallbacks {

        Activity prevPausedOne;

        public void onActivityCreated(Activity activity, Bundle bundle) {

        }

        public void onActivityDestroyed(Activity activity) {

        }

        public void onActivityPaused(Activity activity) {
            prevPausedOne = activity;
        }

        public void onActivityResumed(Activity activity) {
            if (null == prevPausedOne || prevPausedOne == activity) {
                //application launched from background,
                onApplicationLaunchedFromBackground();
            }
        }

        public void onActivitySaveInstanceState(Activity activity,
                                                Bundle outState) {
        }

        public void onActivityStarted(Activity activity) {
        }

        public void onActivityStopped(Activity activity) {
        }
    }
}
