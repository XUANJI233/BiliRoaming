package me.iacn.biliroaming.hook

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.iacn.biliroaming.BiliBiliPackage.Companion.instance
import me.iacn.biliroaming.SettingDialog
import me.iacn.biliroaming.utils.*
import java.lang.reflect.Constructor
import java.lang.reflect.Proxy


class SettingHook(classLoader: ClassLoader) : BaseHook(classLoader) {
    private var startSetting = false

    override fun startHook() {
        Log.d("startHook: Setting")

        instance.splashActivityClass?.hookBeforeMethod("onCreate", Bundle::class.java) { param ->
            val self = param.thisObject as Activity
            startSetting = self.intent.hasExtra(START_SETTING_KEY)
        }

        instance.mainActivityClass?.hookAfterMethod("onResume") { param ->
            if (startSetting) {
                startSetting = false
                SettingDialog(param.thisObject as Activity).show()
            }
        }

        instance.mainActivityClass?.hookBeforeMethod(
            "onCreate",
            Bundle::class.java
        ) { param ->
            val bundle = param.args[0] as? Bundle
            bundle?.remove("android:fragments")
        }

        instance.drawerClass?.hookAfterMethod(
            "onCreateView",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Bundle::class.java
        ) { param ->
            val navSettingId = getId("nav_settings")
            val nav =
                param.thisObject.javaClass.declaredFields.first { it.type.name == "android.support.design.widget.NavigationView" }.name
            (param.thisObject.getObjectField(nav)
                ?: param.result).callMethodAs<View>("findViewById", navSettingId)
                .setOnLongClickListener {
                    SettingDialog(param.thisObject.callMethodAs<Activity>("getActivity")).show()
                    true
                }
        }

        instance.settingRouterClass?.hookBeforeAllConstructors { param ->
            if (param.args[1] != SETTING_URI) return@hookBeforeAllConstructors
            val routerType = (param.method as Constructor<*>).parameterTypes[3]
            param.args[3] = Proxy.newProxyInstance(
                routerType.classLoader,
                arrayOf(routerType)
            ) { _, method, _ ->
                val returnType = method.returnType
                Proxy.newProxyInstance(
                    returnType.classLoader,
                    arrayOf(returnType)
                ) { _, method2, args ->
                    when (method2.returnType) {
                        Boolean::class.javaPrimitiveType -> false
                        else -> {
                            if (method2.parameterTypes.isNotEmpty() &&
                                method2.parameterTypes[0].name == "android.app.Activity"
                            ) {
                                val currentActivity = args[0] as Activity
                                SettingDialog(currentActivity).show()
                            }
                            null
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val START_SETTING_KEY = "biliroaming_start_setting"
        const val SETTING_URI = "bilibili://biliroaming"
        const val SETTING_ID = 114514
    }
}
