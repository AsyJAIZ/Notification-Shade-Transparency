package com.asyjaiz.A12blur;

import static de.robv.android.xposed.XposedHelpers.findConstructorExact;
import static de.robv.android.xposed.XposedHelpers.setFloatField;

import android.content.res.XModuleResources;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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

    /* public static Boolean lowPower() {
        Process process = null;
        BufferedReader bufferedReader = null;

        try {
            process = new ProcessBuilder().command("/system/bin/settings", "get global low_power").start();
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = bufferedReader.readLine();
            XposedBridge.log("Power Saving Mode check returned " + line);
            return line.contains("true") || line.contains("1");
        } catch (Exception e) {
            XposedBridge.log("Power Saving Mode check returned an exception. Returning fail by default");
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
    } */

    /* public static float alpha(boolean capable) {
        float defaultA = 0.85f;
        float supportedA = 0.54f;
        if (capable) {
            if (false)
                return defaultA;
            else return supportedA;
        } else return defaultA;
    } */

    private Constructor<?> findConstructor(boolean forceOld13Constructor, Class<?> hookClass) throws Throwable {
        Object[] parameterTypesAndCallback = new Object[0];
        if (Build.VERSION.SDK_INT >= 33) {
            if (Build.TIME > 1678278441000L && !forceOld13Constructor) {
                if (BuildConfig.DEBUG) {
                    XposedBridge.log("Your device is using a build of the new type.");
                }
                parameterTypesAndCallback = new Object [] {
                        rootPackage + ".statusbar.phone.LightBarController",
                        rootPackage + ".statusbar.phone.DozeParameters",
                        "android.app.AlarmManager",
                        rootPackage + ".statusbar.policy.KeyguardStateController",
                        rootPackage + ".util.wakelock.DelayedWakeLock.Builder",
                        "android.os.Handler",
                        "com.android.keyguard.KeyguardUpdateMonitor",
                        rootPackage + ".dock.DockManager",
                        rootPackage + ".statusbar.policy.ConfigurationController",
                        "java.util.concurrent.Executor",
                        rootPackage + ".statusbar.phone.ScreenOffAnimationController",
                        rootPackage + ".keyguard.KeyguardUnlockAnimationController",
                        rootPackage + ".statusbar.phone.StatusBarKeyguardViewManager",
                        rootPackage + ".keyguard.ui.viewmodel.PrimaryBouncerToGoneTransitionViewModel",
                        rootPackage + ".keyguard.domain.interactor.KeyguardTransitionInteractor",
                        "kotlinx.coroutines.CoroutineDispatcher",
                        rootPackage + ".shade.transition.LargeScreenShadeInterpolator",
                        rootPackage + ".flags.FeatureFlags"
                };
                try {
                    return findConstructorExact(hookClass, parameterTypesAndCallback);
                } catch (Throwable t) {
                    if (BuildConfig.DEBUG) {
                        XposedBridge.log("Problem with finding corresponding constructor, retrying with an old scheme");
                    }
                    return findConstructor(true, hookClass);
                }
            } else {
                parameterTypesAndCallback = new Object [] {
                        rootPackage + ".statusbar.phone.LightBarController",
                        rootPackage + ".statusbar.phone.DozeParameters",
                        "android.app.AlarmManager",
                        rootPackage + ".statusbar.policy.KeyguardStateController",
                        rootPackage + ".util.wakelock.DelayedWakeLock.Builder",
                        "android.os.Handler",
                        "com.android.keyguard.KeyguardUpdateMonitor",
                        rootPackage + ".dock.DockManager",
                        rootPackage + ".statusbar.policy.ConfigurationController",
                        "java.util.concurrent.Executor",
                        rootPackage + ".statusbar.phone.ScreenOffAnimationController",
                        rootPackage + ".keyguard.KeyguardUnlockAnimationController",
                        rootPackage + ".statusbar.phone.StatusBarKeyguardViewManager",
                };
            }
        }
        else if (Build.VERSION.SDK_INT == 32) {
            parameterTypesAndCallback = new Object [] {
                rootPackage + ".statusbar.phone.LightBarController",
                rootPackage + ".statusbar.phone.DozeParameters",
                "android.app.AlarmManager",
                rootPackage + ".statusbar.policy.KeyguardStateController",
                rootPackage + ".util.wakelock.DelayedWakeLock.Builder",
                "android.os.Handler",
                "com.android.keyguard.KeyguardUpdateMonitor",
                rootPackage + ".dock.DockManager",
                rootPackage + ".statusbar.policy.ConfigurationController",
                "java.util.concurrent.Executor",
                rootPackage + ".statusbar.phone.UnlockedScreenOffAnimationController",
                rootPackage + ".statusbar.phone.panelstate.PanelExpansionStateManager"
            };
        }
        else if (Build.VERSION.SDK_INT == 31) {
            parameterTypesAndCallback = new Object [] {
                rootPackage + ".statusbar.phone.LightBarController",
                rootPackage + ".statusbar.phone.DozeParameters",
                "android.app.AlarmManager",
                rootPackage + ".statusbar.policy.KeyguardStateController",
                rootPackage + ".util.wakelock.DelayedWakeLock.Builder",
                "android.os.Handler",
                "com.android.keyguard.KeyguardUpdateMonitor",
                rootPackage + ".dock.DockManager",
                rootPackage + ".statusbar.policy.ConfigurationController",
                "java.util.concurrent.Executor",
                rootPackage + ".statusbar.phone.UnlockedScreenOffAnimationController"
            };
        }
        return findConstructorExact(hookClass, parameterTypesAndCallback);
    }

    private Constructor<?> findConstructor(Class<?> hookClass) throws Throwable {
        return findConstructor(false, hookClass);
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

        supportsBlur = read("ro.surface_flinger.supports_background_blur");
        avoidAccel2 = read("ro.config.avoid_gfx_accel");
        lowRam = read("ro.config.low_ram");
        disableBlur = read("persist.sysui.disableBlur");
        boolean capable = supportsBlur && !avoidAccel1 && !avoidAccel2 && !lowRam && !disableBlur;
        if (BuildConfig.DEBUG) XposedBridge.log(capable ?
                "Your device is capable of handling blur. Using a corresponding alpha value" :
                "Your device is not capable of handling blur. Using a default alpha value");
        float alpha = capable ? 0.54f : 0.85f;

        final Class<?> ScrimController = XposedHelpers.findClass(rootPackage + ".statusbar.phone.ScrimController", lpParam.classLoader);
        XC_MethodHook xcMethodHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setFloatField(param.thisObject, "mDefaultScrimAlpha", alpha);
            }
        };

        if (BuildConfig.DEBUG) XposedBridge.log("Trying to hook a ScrimController constructor.");
        XposedBridge.hookMethod(findConstructor(ScrimController), xcMethodHook);
    }
}

