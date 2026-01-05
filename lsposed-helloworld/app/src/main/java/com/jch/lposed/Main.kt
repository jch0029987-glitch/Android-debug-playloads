package com.jch.lposed
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

class Main(module: XposedModule, classLoader: ClassLoader) : XposedModule(module, classLoader) {

    override fun onPackageLoaded(param: XposedInterface.PackageLoadedParam) {
        if (!param.isFirstPackage) return

        log("Hello World from LSPosed Module (Modern API)!")
        log("Module active in package: ${param.packageName}")
    }
}
