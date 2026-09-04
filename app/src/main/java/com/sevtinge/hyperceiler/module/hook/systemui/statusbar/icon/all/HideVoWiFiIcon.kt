/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import com.sevtinge.hyperceiler.module.base.*
import de.robv.android.xposed.XposedHelpers

object HideVoWiFiIcon : BaseHook() {
    override fun init() {
        val hideVoWifi by lazy {
            mPrefsMap.getBoolean("system_ui_status_bar_icon_vowifi")
        }
        val hideVolte by lazy {
            mPrefsMap.getBoolean("system_ui_status_bar_icon_volte")
        }
        val mobileView = findClassIfExists(
            "com.android.systemui.statusbar.StatusBarMobileView",
            lpparam.classLoader
        )
        val mobileState = findClassIfExists(
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy\$MobileIconState",
            lpparam.classLoader
        )
        if (mobileView == null || mobileState == null) {
            logE(TAG, lpparam.packageName, "Cannot find mobile view or state")
            return
        }

        val stateHook = object : MethodHook() {
            override fun before(param: MethodHookParam) {
                val state = param.args.firstOrNull() ?: return
                if (hideVoWifi) XposedHelpers.setBooleanField(state, "hideVowifi", true)
                if (hideVolte) XposedHelpers.setBooleanField(state, "hideVolte", true)
            }

            override fun after(param: MethodHookParam) {
                if (hideVoWifi) hideViewIfPresent(param.thisObject, "mVowifi")
                if (hideVolte) hideViewIfPresent(param.thisObject, "mVolte")
            }
        }

        findAndHookMethodSilently(mobileView, "initViewState", mobileState, stateHook)
        findAndHookMethodSilently(mobileView, "applyMobileState", mobileState, stateHook)
        findAndHookMethodSilently(mobileView, "updateState", mobileState, stateHook)
    }

    private fun hideViewIfPresent(target: Any, fieldName: String) {
        runCatching {
            val view = XposedHelpers.getObjectField(target, fieldName) as? android.view.View
            view?.visibility = android.view.View.GONE
        }
    }
}
