package com.jch.lsposed

import android.app.Activity
import android.widget.Toast
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class ModuleMain(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    override fun onPackageLoaded(param: PackageLoadedParam) {
        // Show toast in every app
        log("📱 Hooking: ${param.packageName}")
        
        try {
            // Hook Activity.onCreate to show toast
            findAndHookMethod(
                "android.app.Activity",
                param.classLoader,
                "onCreate",
                android.os.Bundle::class.java,
                object : XposedInterface.HookCallback {
                    override fun before(callback: XposedInterface.BeforeHookCallback) {
                        // Nothing before
                    }
                    
                    override fun after(callback: XposedInterface.AfterHookCallback) {
                        try {
                            val activity = callback.thisObject as Activity
                            activity.runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    "Hello from JCH! 👋",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            log("✅ Toast shown in ${param.packageName}")
                        } catch (e: Exception) {
                            // Ignore errors
                        }
                    }
                }
            )
            
        } catch (e: Exception) {
            log("❌ Hook failed: ${e.message}")
        }
    }
}
