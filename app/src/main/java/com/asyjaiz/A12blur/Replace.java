package com.asyjaiz.A12blur;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findField;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static de.robv.android.xposed.XposedHelpers.setFloatField;

import android.content.Context;
import android.content.res.XModuleResources;
import android.graphics.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Replace implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    String rootPackage = "com.android.systemui";

    boolean supportsBlur;
    boolean avoidAccel1;
    boolean avoidAccel2;
    boolean lowRam;
    boolean disableBlur;

    public static Boolean read(String propName) {
        Process process = null;
        BufferedReader bufferedReader = null;

        try {
            process = new ProcessBuilder().command("/system/bin/getprop", propName).start();
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = bufferedReader.readLine();
            return line.contains("true") || line.contains("1");
        } catch (Exception e) {
            log("Something went wrong: " + e + "\nMake a new issue on a Github page.");
            return false;
        } finally {
            if (bufferedReader != null){
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {}
            }
            if (process != null){
                process.destroy();
            }
        }
    }

    private Constructor<?> findConstructor(Class<?> hookClass) throws Throwable {
        Constructor<?>[] constructors = hookClass.getConstructors();
        if (constructors.length >= 1) {
            return constructors[0];
        } else {
            throw new NoSuchMethodException("Did not find constructor for " + hookClass.getName());
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resParam) throws Throwable {
        XModuleResources modRes = XModuleResources.createInstance(resParam.packageName, null);
        avoidAccel1 = modRes.getBoolean(modRes.getIdentifier("config_avoidGfxAccel", "bool", "android"));
    }

    static float GAR = 0.54f;
    public static void quickCheck(float value) {
        if (GAR > value) log("You may want to set value " + value + "to " + GAR + "or higher for better text visibility");
    }

    static String launcher = "com.android.launcher3";
    static String quickstep = "com.android.quickstep";
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
        if ((lpParam.packageName.equals(launcher)) ||
                (lpParam.packageName.equals("com.google.android.apps.nexuslauncher"))) {
            findAndHookMethod(quickstep + ".fallback.RecentsState", lpParam.classLoader,
                    "getScrimColor", Context.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            param.setResult((int) Color.BLUE);
                            log("Modified result successfully");
                        }
                    });
        }
        else if (!lpParam.packageName.equals(rootPackage)) {
            return;
        }

        if (BuildConfig.DEBUG) {
            log("Debug and diagnostics.");
        }


        XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "set");
        boolean readable = BuildConfig.FLAVOR.equals("gui") && prefs.getFile().canRead();
        if (BuildConfig.DEBUG)
            log("Can read preferences? " + readable);

        float notifAlpha;
        boolean auto = prefs.getBoolean("auto", true);
        if (!readable || auto) {
            supportsBlur = read("ro.surface_flinger.supports_background_blur");
            avoidAccel2 = read("ro.config.avoid_gfx_accel");
            lowRam = read("ro.config.low_ram");
            disableBlur = read("persist.sysui.disableBlur");
            boolean capable = supportsBlur && !avoidAccel1 && !avoidAccel2 && !lowRam && !disableBlur; // yes, sadly we can't just call BlurUtils check for this, so we should do this on our own
            if (BuildConfig.DEBUG) log(capable ?
                    "Your device is capable of handling blur. Using a corresponding alpha value" :
                    "Your device is not capable of handling blur. Using a default alpha value");
            if (!capable && BuildConfig.DEBUG) {
                StringBuilder builder = new StringBuilder("Possible culprits:\n");
                if (!supportsBlur)
                    builder.append("    Your SurfaceFlinger may have blur disabled\n");
                if (avoidAccel1 || avoidAccel2)
                    builder.append("    System avoids graphical acceleration\n");
                if (lowRam)
                    builder.append("    Device reports to be a low ram device\n");
                if (disableBlur)
                    builder.append("    Blur is disabled in SystemUI by a property");
                log(builder.toString());
            }
            notifAlpha = capable ? GAR : 0.85f;
        } else {
            notifAlpha = prefs.getFloat("notif_alpha", 1.0f);
            quickCheck(notifAlpha);
        }

        String base = ".statusbar.phone.";
        final Class<?> ScrimController = findClass(rootPackage + base + "ScrimController", lpParam.classLoader);
        XC_MethodHook xcMethodHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setFloatField(param.thisObject, "mDefaultScrimAlpha", notifAlpha); // Override BUSY_SCRIM_ALPHA
            }
        };

        if (BuildConfig.DEBUG) log("Trying to hook a ScrimController constructor.");
        hookMethod(findConstructor(ScrimController), xcMethodHook);

        if (!readable || auto) return; // Yes, exactly. You see here locked out functionality.
        float alpha = prefs.getFloat("behind_alpha", 1f);
        quickCheck(alpha);
        /*if (BuildConfig.DEBUG) log("Hooking ScrimState");
        final Class<?> ScrimState = findClass(rootPackage + base + "ScrimState", lpParam.classLoader);
        final Class<?> ScrimView = findClass(rootPackage + ".scrim.ScrimView", lpParam.classLoader);
        Method updateScrimColor;
        String name = "updateScrimColor";
        boolean stripped;
        try {
            updateScrimColor = findMethodBestMatch(ScrimState, name, ScrimView, float.class, int.class);
            stripped = false;
        } catch (NoSuchMethodError exception) {
            try {
                updateScrimColor = findMethodBestMatch(ScrimState, name, ScrimView);
            } catch (NoSuchMethodError exception2) {
                updateScrimColor = findMethodBestMatch(ScrimState, name, ScrimView, int.class);
            }
            stripped = true;
        }

        final boolean finalStripped = stripped;
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object mScrimBehind = findField(ScrimState, "mScrimBehind").get(param.thisObject);
                if (!mScrimBehind.equals(param.args[0]))
                    return;

                if (!finalStripped)
                    if ((Float) param.args[1] == 1f) {
                        param.args[1] = alpha;
                        //log("Alpha is " + param.args[1]);
                    }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object mScrimBehind = findField(ScrimState, "mScrimBehind").get(param.thisObject);

                if (finalStripped) {
                    callMethod(mScrimBehind, "setViewAlpha", alpha);
                    //log("Alpha is " + (Float) callMethod(mScrimBehind, "getViewAlpha"));
                }
            }
        };
        if (BuildConfig.DEBUG) log("Hooking updateScrimColor");
        hookMethod(updateScrimColor, hook);*/

        if (BuildConfig.DEBUG) log("Hooking applyState in ScrimController");
        Method applyState;
        try {
            applyState = findMethodBestMatch(ScrimController, "applyState");
        } catch (NoSuchMethodError e) {
            try {
                applyState = findMethodBestMatch(ScrimController, "applyState$1");
            } catch (NoSuchMethodError ex) {
                try {
                    applyState = findMethodBestMatch(ScrimController, "applyState$1$1");
                } catch (NoSuchMethodError exc) {
                    log("applyState function was not found. Please open a new GitHub issue and provide next info:\n1. OS version\n2. Error log\n3. SystemUI folder copy");
                    throw new RuntimeException(exc);
                }
            }
        }
        hookMethod(applyState, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Field mBehindAlpha = findField(ScrimController, "mBehindAlpha");
                if (mBehindAlpha.getFloat(param.thisObject) == 1f) {
                    mBehindAlpha.setFloat(param.thisObject, alpha);
                    // TODO: Tint
                }
            }
        });
    }
}

