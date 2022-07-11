package com.asyjaiz.A12blur;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findField;

import android.os.Build;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

// public class Replace implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

public class Replace implements IXposedHookLoadPackage {
    String pkg = "com.android.systemui";
    //private static String MODULE_PATH = null;

    public boolean versionCheck() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    //@Override
    //public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        //MODULE_PATH = startupParam.modulePath;
    //}

    //@Override
    //public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        //if (!resparam.packageName.equals(pkg) || !versionCheck())
            //return;

        //XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        //resparam.res.setReplacement(pkg, "dimen", "max_window_blur_radius", modRes.fwd(R.dimen.max_window_blur_radius));
    //}

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(pkg) || !versionCheck())
            return;

        final Class<?> findClass = XposedHelpers.findClass(pkg + ".statusbar.phone.ScrimController", lpparam.classLoader);

        findAndHookMethod(findClass, "updateScrimColor", View.class, float.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!XposedHelpers.findField(findClass, "mNotificationsScrim").get(param.thisObject).equals(param.args[0]))
                    return;

                param.args[1] = (Float) param.args[1] * 0.0f;
                param.args[2] = -16777216;

            }
        });
    }
}

