package com.asyjaiz.A12blur;

import static com.asyjaiz.A12blur.MainActivity.scrimList;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.os.Build;
import android.view.View;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Replace implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {
    String pkg = "com.android.systemui";
    static XSharedPreferences prefs;
    //Context context;

    //public Replace(Context context) {
        //this.context = context;

    //}

    public boolean versionCheck() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S;
    }

    ArrayList<String> done = new ArrayList<>();

    public void apply(XC_MethodHook.MethodHookParam param, Class<?> findClass) throws Throwable {
        done.clear();
        for (String scrim : scrimList) {
            //XposedBridge.log(scrim + " " + param.args[1] + " " + param.args[2]);
            if (!XposedHelpers.findField(findClass, scrim).get(param.thisObject).equals(param.args[0]))
                continue;

            if (done.contains(scrim))
                continue;

            //param.args[1] = (Float) param.args[1] * (Float) entry.getValue();
            param.args[1] = (Float) param.args[1] * prefs.getFloat(scrim, 1.0f);
            param.args[2] = -16777216;
            done.add(scrim);
        }
    }

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(startupParam.modulePath, "MainActivity");
        //prefs.getFile().setReadable(true);
        //prefs = new XSharedPreferences(startupParam.modulePath, "MainActivity");
        prefs = prefs.getFile().canRead() ? prefs : null;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(pkg) || versionCheck())
            return;
        if (prefs == null) {
            //XposedBridge.log("Shared Preferences using may produce NullPointerException.");
            prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "MainActivity");
            if (!prefs.getFile().canRead()) {
                //XposedBridge.log("Still can't read. Return");
                return;
            }
            //else XposedBridge.log("Pass");
        }

        //XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        //resparam.res.setReplacement(pkg, "dimen", "max_window_blur_radius", new XResources.DimensionReplacement(prefs.getFloat("max_window_blur_radius", maxBlurDefault), TypedValue.COMPLEX_UNIT_PX));
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(pkg) || versionCheck())
            return;
        if (prefs == null) {
            //XposedBridge.log("Shared Preferences using may produce NullPointerException.");
            prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "MainActivity");
            if (!prefs.getFile().canRead()) {
                //XposedBridge.log("Still can't read. Return");
                return;
            }
            //else XposedBridge.log("Pass");
        }

        final Class<?> findClass = XposedHelpers.findClass(pkg + ".statusbar.phone.ScrimController", lpparam.classLoader);
        //Field[] scrimView = {XposedHelpers.findField(findClass, scrimList[0]), XposedHelpers.findField(findClass, scrimList[1]), XposedHelpers.findField(findClass, scrimList[2]), XposedHelpers.findField(findClass, scrimList[3])};

        findAndHookMethod(findClass, "updateScrimColor", View.class, float.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                apply(param, findClass);
            }
        });
    }
}

