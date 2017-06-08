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

package org.catrobat.catroid.test.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;

import org.catrobat.catroid.ui.SettingsActivity;
import org.catrobat.catroid.utils.CrashReporter;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class CrashReporterTest {

	private Context context;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;
	private Exception exception;

	@Before
	public void setUp() {
		context = InstrumentationRegistry.getTargetContext();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		editor = sharedPreferences.edit();
		editor.clear();
		editor.commit();
		exception = new RuntimeException("Error");
	}

	@Test
	public void testCrashlyticsUninitializedOnAnonymousReportDisabled() {
		editor.putBoolean(SettingsActivity.SETTINGS_CRASH_REPORTS, false);
		editor.commit();

		CrashReporter.setIsCrashReportEnabled(true);

		assertFalse(CrashReporter.initialize(context));
	}

	@Test
	public void testCrashlyticsInitializedOnAnonymousReportEnabled() {
		editor.putBoolean(SettingsActivity.SETTINGS_CRASH_REPORTS, true);
		editor.commit();

		CrashReporter.setIsCrashReportEnabled(true);

		assertTrue(CrashReporter.initialize(context));
	}

	@Test
	public void testUnhandledExceptionStoredOnCrashReportEnabled() {
		CrashReporter.setIsCrashReportEnabled(true);
		CrashReporter.storeUnhandledException(exception);

		assertTrue(!sharedPreferences.getString(CrashReporter.EXCEPTION_FOR_REPORT, "").isEmpty());
	}

	@Test
	public void testUnhandledExceptionNotStoredOnCrashReportDisabled() {
		CrashReporter.setIsCrashReportEnabled(false);
		CrashReporter.storeUnhandledException(exception);

		assertTrue(sharedPreferences.getString(CrashReporter.EXCEPTION_FOR_REPORT, "").isEmpty());
	}

	@Test
	public void testUnhandledExceptionStoredOnNoPreviousExceptionStored() {
		assertTrue(sharedPreferences.getString(CrashReporter.EXCEPTION_FOR_REPORT, "").isEmpty());

		CrashReporter.setIsCrashReportEnabled(true);
		CrashReporter.storeUnhandledException(exception);

		assertTrue(!sharedPreferences.getString(CrashReporter.EXCEPTION_FOR_REPORT, "").isEmpty());
	}

	@Test
	public void testSharedPreferencesClearedAfterLoggingException() {
		CrashReporter.setIsCrashReportEnabled(true);
		CrashReporter.storeUnhandledException(exception);

		assertTrue(!sharedPreferences.getString(CrashReporter.EXCEPTION_FOR_REPORT, "").isEmpty());

		CrashReporter.sendUnhandledCaughtException();

		assertTrue(sharedPreferences.getString(CrashReporter.EXCEPTION_FOR_REPORT, "").isEmpty());
	}

	@Test
	public void testLogExceptionGenerateLogsOnReportsEnabled() {
		editor.putBoolean(SettingsActivity.SETTINGS_CRASH_REPORTS, true);
		editor.commit();

		CrashReporter.setIsCrashReportEnabled(true);
		CrashReporter.initialize(context);

		assertTrue(CrashReporter.logException(exception));
	}

	@Test
	public void testLogExceptionGenerateNoLogsOnReportsDisabled() {
		editor.putBoolean(SettingsActivity.SETTINGS_CRASH_REPORTS, false);
		editor.commit();

		CrashReporter.setIsCrashReportEnabled(false);
		CrashReporter.initialize(context);

		assertFalse(CrashReporter.logException(exception));
	}
}
