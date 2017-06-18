/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2017 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.gson.Gson;

import org.catrobat.catroid.BuildConfig;
import org.catrobat.catroid.ui.MainMenuActivity;
import org.catrobat.catroid.ui.SettingsActivity;

import io.fabric.sdk.android.Fabric;

public final class CrashReporter {

	private static final String TAG = CrashReporter.class.getSimpleName();

	public static final String RECOVERED_FROM_CRASH = "RECOVERED_FROM_CRASH";
	public static final String EXCEPTION_FOR_REPORT = "EXCEPTION_FOR_REPORT";

	static Context context;
	private static SharedPreferences preferences;
	private static boolean isCrashReportEnabled;
	private static boolean isCrashlyticsInitialized;

	private CrashReporter() {
	}

	public static boolean initialize(Context context) {

		CrashReporter.context = context;
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		setIsCrashReportEnabled(BuildConfig.CRASHLYTICS_CRASH_REPORT_ENABLED);
		isCrashlyticsInitialized = false;

		if (isAnonymousReportEnabled() && isCrashReportEnabled) {
			Fabric.with(context, new Crashlytics());
			Log.d(TAG, "INITIALIZED!");
			isCrashlyticsInitialized = true;
			return true;
		}
		Log.d(TAG, "INITIALIZATION FAILED! [ Anonymous Report : " + isAnonymousReportEnabled() + " Crash Report : " + isCrashReportEnabled + " ]");
		isCrashlyticsInitialized = false;
		return false;
	}

	private static boolean isAnonymousReportEnabled() {
		return preferences != null && preferences.getBoolean(SettingsActivity.SETTINGS_CRASH_REPORTS, false);
	}

	private static boolean isRecoveredFromCrash() {
		return preferences != null && preferences.getBoolean(RECOVERED_FROM_CRASH, false);
	}

	public static void disableCrashlytics() {
		if (isCrashlyticsInitialized) {
			Crashlytics crashlytics = new Crashlytics.Builder()
					.core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
					.build();
			Fabric.with(context, crashlytics);
			isCrashlyticsInitialized = false;
		}
	}

	@VisibleForTesting
	public static void setIsCrashReportEnabled(boolean value) {
		isCrashReportEnabled = value;
	}

	public static boolean logException(Throwable t) {

		if (isAnonymousReportEnabled() && isCrashReportEnabled) {
			Log.e(TAG, "LOG_EXCEPTION : " + t);
			Crashlytics.logException(t);
			return true;
		}

		return false;
	}

	public static boolean checkIfCrashRecoveryAndFinishActivity(final Activity context) {
		Log.d(TAG, "AFTER_EXCEPTION : checkIfCrashRecoveryAndFinishActivity()");
		if (isRecoveredFromCrash()) {
			if (isCrashReportEnabled && isAnonymousReportEnabled()) {
				sendUnhandledCaughtException();
			}
			if (!(context instanceof MainMenuActivity)) {
				context.finish();
			} else {
				preferences.edit().putBoolean(RECOVERED_FROM_CRASH, false).commit();
				return true;
			}
		}
		return false;
	}

	public static void sendUnhandledCaughtException() {
		Log.d(TAG, "AFTER_EXCEPTION : sendCaughtException()");
		Gson gson = new Gson();
		String json = preferences.getString(EXCEPTION_FOR_REPORT, "");
		Throwable exception = gson.fromJson(json, Throwable.class);
		logException(exception);
		preferences.edit().remove(EXCEPTION_FOR_REPORT).commit();
	}

	public static void storeUnhandledException(Throwable exception) {
		SharedPreferences.Editor prefsEditor = preferences.edit();
		if (isCrashReportEnabled) {
			Gson gson = new Gson();
			String check = preferences.getString(EXCEPTION_FOR_REPORT, "");
			if (check.isEmpty()) {
				String json = gson.toJson(exception);
				prefsEditor.putString(EXCEPTION_FOR_REPORT, json);
				prefsEditor.commit();
			}
		}
		preferences.edit().putBoolean(RECOVERED_FROM_CRASH, true).commit();
	}
}
