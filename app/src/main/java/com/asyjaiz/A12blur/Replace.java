package com.asyjaiz.A12blur;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static de.robv.android.xposed.XposedHelpers.setFloatField;

import android.content.res.XModuleResources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Replace implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    String rootPackage = "com.android.systemui";
    private String rootPackagePath;

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
            boolean contains = line.contains("true") || line.contains("1");
            if (BuildConfig.DEBUG) XposedBridge.log("Check for " + propName + " returned " +
                    line +
                    (contains ? " and contains a success value" : " and doesn't contain a success value"));
            return contains;
        } catch (Exception e) {
            XposedBridge.log("Something went wrong: " + e + "\nMake a new issue on a Github page.");
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
        XModuleResources modRes = XModuleResources.createInstance(rootPackagePath, null);
        avoidAccel1 = modRes.getBoolean(modRes.getIdentifier("config_avoidGfxAccel", "bool", "android"));
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
        if (!lpParam.packageName.equals(rootPackage))
            return;

        if (BuildConfig.DEBUG) {
            XposedBridge.log("Debug and diagnostics.");
        }

        rootPackagePath = lpParam.appInfo.sourceDir;

        XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "set");
        if (BuildConfig.DEBUG)
            XposedBridge.log("Can read preferences? " + prefs.getFile().canRead());

        float notifAlpha;
        if (prefs.getBoolean("auto", true)) {
            supportsBlur = read("ro.surface_flinger.supports_background_blur");
            avoidAccel2 = read("ro.config.avoid_gfx_accel");
            lowRam = read("ro.config.low_ram");
            disableBlur = read("persist.sysui.disableBlur");
            boolean capable = supportsBlur && !avoidAccel1 && !avoidAccel2 && !lowRam && !disableBlur; // yes, sadly we can't just call BlurUtils check for this, so we should do this on our own
            if (BuildConfig.DEBUG) XposedBridge.log(capable ?
                    "Your device is capable of handling blur. Using a corresponding alpha value" :
                    "Your device is not capable of handling blur. Using a default alpha value");
            notifAlpha = capable ? 0.54f : 0.85f;
        } else {
            notifAlpha = prefs.getFloat("notif_alpha", 1.0f);
        }

        String base = ".statusbar.phone.";
        final Class<?> ScrimController = XposedHelpers.findClass(rootPackage + base + "ScrimController", lpParam.classLoader);
        XC_MethodHook xcMethodHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setFloatField(param.thisObject, "mDefaultScrimAlpha", notifAlpha); // Override BUSY_SCRIM_ALPHA
            }
        };

        if (BuildConfig.DEBUG) XposedBridge.log("Trying to hook a ScrimController constructor.");
        XposedBridge.hookMethod(findConstructor(ScrimController), xcMethodHook);

        if (BuildConfig.DEBUG) XposedBridge.log("Hooking ScrimState");
        final Class<?> ScrimState = findClass(rootPackage + base + "ScrimState", lpParam.classLoader);
        final Class<?> ScrimView = findClass(rootPackage + ".scrim.ScrimView", lpParam.classLoader);
        Method updateScrimColor;
        XC_MethodHook hook;
        try {
            updateScrimColor = findMethodBestMatch(ScrimState, "updateScrimColor", ScrimView, float.class, int.class);
            hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!XposedHelpers.findField(ScrimState, "mScrimBehind").get(param.thisObject).equals(param.args[0]))
                        return;

                    if ((Float) param.args[1] == 1f)
                        param.args[1] = prefs.getFloat("behind_alpha", 1f);
                }
            };
        } catch (NoSuchMethodError exception) {

        }
        XposedBridge.hookMethod(updateScrimColor, hook);
    }
}

