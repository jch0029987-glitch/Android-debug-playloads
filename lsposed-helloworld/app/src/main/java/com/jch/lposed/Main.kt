package com.jch.lposed

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

/**
 * Main entry point for the LSPosed module.
 * This class is instantiated by the framework when the module is loaded.
 */
class Main(module: XposedModule, classLoader: ClassLoader) : XposedModule(module, classLoader) {

    override fun onPackageLoaded(param: XposedInterface.PackageLoadedParam) {
        // Only execute once per process (when the first package in the scope loads)
        if (!param.isFirstPackage) return

        // Your custom Hello World messages
        log("==========================================")
        log("Hello World from JCH's Custom LSPosed Module!")
        log("Module loaded successfully!")
        log("Process: ${param.processName}")
        log("Package: ${param.packageName}")
        log("==========================================")
    }

    // Optional: Log when the module loads in system_server (if scoped to system)
    override fun onSystemServerLoaded(param: XposedInterface.SystemServerLoadedParam) {
        log("Module loaded in system_server!")
    }
}
