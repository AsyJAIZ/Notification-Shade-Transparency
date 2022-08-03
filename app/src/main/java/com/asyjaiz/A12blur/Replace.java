package com.asyjaiz.A12blur;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.setFloatField;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Replace implements IXposedHookLoadPackage {
    String pkg = "com.android.systemui";
    private final boolean tint = true; // I do not recommend disabling tint, text will become unreadable
    private boolean supportsBlur = false;
    private boolean rehook = false;

    public boolean versionCheck() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(pkg) || !versionCheck())
            return;

        final Class<?> ScrimController = XposedHelpers.findClass(pkg + ".statusbar.phone.ScrimController", lpparam.classLoader);
        final Class<?> BlurUtils = XposedHelpers.findClass(pkg + ".statusbar.BlurUtils", lpparam.classLoader);

        findAndHookMethod(BlurUtils, "supportsBlursOnWindows", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                supportsBlur = (boolean) param.getResult();
                //XposedBridge.log("Supports blur? " + (supportsBlur ? "Yes" : "No"));
            }
        });

        findAndHookConstructor(ScrimController, pkg + ".statusbar.phone.LightBarController",
                pkg + ".statusbar.phone.DozeParameters",
                "android.app.AlarmManager",
                pkg + ".statusbar.policy.KeyguardStateController",
                pkg + ".util.wakelock.DelayedWakeLock.Builder",
                "android.os.Handler",
                "com.android.keyguard.KeyguardUpdateMonitor",
                pkg + ".dock.DockManager",
                pkg + ".statusbar.policy.ConfigurationController",
                "java.util.concurrent.Executor",
                pkg + ".statusbar.phone.UnlockedScreenOffAnimationController",
                pkg + ".statusbar.phone.panelstate.PanelExpansionStateManager",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        float tintF = tint ? (supportsBlur ? 0.54f : 0.85f) : 0f;
                        setFloatField(param.thisObject, "mDefaultScrimAlpha", tintF);
                        XposedBridge.log("Tint alpha value: " + tintF);
                    }
                });
    }
}

