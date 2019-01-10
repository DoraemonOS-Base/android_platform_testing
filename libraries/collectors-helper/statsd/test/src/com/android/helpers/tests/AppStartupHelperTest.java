/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.helpers.tests;

import android.os.SystemClock;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.ICalendarHelper;
import androidx.test.runner.AndroidJUnit4;

import com.android.helpers.AppStartupHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Android Unit tests for {@link AppStartupHelper}.
 *
 * To run:
 * Connect to wifi and login to gmail.
 * Disable SELinux: adb shell setenforce 0; if this fails with "permission denied",
 * try "adb shell su 0 setenforce 0"
 * atest CollectorsHelperTest:com.android.helpers.tests.AppStartupHelperTest
 */
@RunWith(AndroidJUnit4.class)
public class AppStartupHelperTest {

    // Kill the calendar app.
    private static final String KILL_TEST_APP_CMD_TEMPLATE = "am force-stop %s";
    // Package names used for testing.
    private static final String CALENDAR_PKG_NAME = "com.google.android.calendar";
    private static final String SETTINGS_PKG_NAME = "com.android.settings";
    // Key prefixes to store the cold, warm or hot launch time of the calendar app, respectively.
    private static final String COLD_LAUNCH_KEY_TEMPLATE = "cold_startup_%s";
    private static final String COLD_LAUNCH_COUNT_PKG_KEY_TEMPLATE = "cold_startup_count_%s";
    private static final String COLD_LAUNCH_TOTAL_COUNT_KEY_TEMPLATE = "cold_startup_total_count";
    private static final String WARM_LAUNCH_KEY_TEMPLATE = "warm_startup_%s";
    private static final String HOT_LAUNCH_KEY_TEMPLATE = "hot_startup_%s";
    // Keyword for keys to store the app startup fully drawn metric.
    private static final String FULLY_DRAWN_KEY_KEYWORD = "fully_drawn";

    // Keycode for pressing the home button.
    private static final String KEYCODE_HOME = "KEYCODE_HOME";

    private AppStartupHelper mAppStartupHelper = new AppStartupHelper();
    private HelperAccessor<ICalendarHelper> mHelper =
            new HelperAccessor<>(ICalendarHelper.class);

    @Before
    public void setUp() {
        mAppStartupHelper = new AppStartupHelper();
        // Make sure the apps are starting from the clean state.
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, CALENDAR_PKG_NAME));
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, SETTINGS_PKG_NAME));
        // Make sure display is on and unlocked.
        HelperTestUtility.wakeUpAndUnlock();
    }

    /**
     * Test successfull app launch config.
     */
    @Test
    public void testAppLaunchConfig() throws Exception {
        assertTrue(mAppStartupHelper.startCollecting());
        assertTrue(mAppStartupHelper.stopCollecting());
    }

    /**
     * Test no error is thrown if there is no app launch.
     */
    @Test
    public void testEmptyAppLaunchMetric() throws Exception {
        assertTrue(mAppStartupHelper.startCollecting());
        assertTrue(mAppStartupHelper.getMetrics().isEmpty());
        assertTrue(mAppStartupHelper.stopCollecting());
    }

    /**
     * Test single cold launch metric.
     */
    @Test
    public void testSingleColdLaunchMetric() throws Exception {
        assertTrue(mAppStartupHelper.startCollecting());
        mHelper.get().open();
        Map<String, StringBuilder> appLaunchMetrics = mAppStartupHelper.getMetrics();
        // A metric key for the app cold launching should exist, and should only hold one value.
        String coldLaunchMetricKey = String.format(COLD_LAUNCH_KEY_TEMPLATE, CALENDAR_PKG_NAME);
        String coldLaunchCountPkgKey = String.format(COLD_LAUNCH_COUNT_PKG_KEY_TEMPLATE,
                CALENDAR_PKG_NAME);
        assertTrue(appLaunchMetrics.keySet().contains(coldLaunchMetricKey));
        assertEquals(1, appLaunchMetrics.get(coldLaunchMetricKey).toString().split(",").length);
        assertEquals(1, Integer.parseInt(appLaunchMetrics.get(coldLaunchCountPkgKey).toString()));
        assertEquals(1, Integer.parseInt(appLaunchMetrics.get(COLD_LAUNCH_TOTAL_COUNT_KEY_TEMPLATE)
                .toString()));
        assertTrue(mAppStartupHelper.stopCollecting());
        mHelper.get().exit();
    }

    /**
     * Test multiple cold launch metric.
     */
    @Test
    public void testMultipleColdLaunchMetric() throws Exception {
        assertTrue(mAppStartupHelper.startCollecting());
        mHelper.get().open();
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        mHelper.get().exit();
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, CALENDAR_PKG_NAME));
        mHelper.get().open();
        Map<String, StringBuilder> appLaunchMetrics = mAppStartupHelper.getMetrics();
        // A metric key for the app cold launching should exist, and should hold two values.
        String coldLaunchMetricKey = String.format(COLD_LAUNCH_KEY_TEMPLATE, CALENDAR_PKG_NAME);
        String coldLaunchCountPkgKey = String.format(COLD_LAUNCH_COUNT_PKG_KEY_TEMPLATE,
                CALENDAR_PKG_NAME);
        assertTrue(appLaunchMetrics.keySet().contains(coldLaunchMetricKey));
        assertEquals(2, appLaunchMetrics.get(coldLaunchMetricKey).toString().split(",").length);
        assertEquals(2, Integer.parseInt(appLaunchMetrics.get(coldLaunchCountPkgKey).toString()));
        assertEquals(2, Integer.parseInt(appLaunchMetrics.get(COLD_LAUNCH_TOTAL_COUNT_KEY_TEMPLATE)
                .toString()));
        assertTrue(mAppStartupHelper.stopCollecting());
        mHelper.get().exit();
    }

    /**
     * Test cold launch metric of two different apps.
     */
    @Test
    public void testDifferentAppColdLaunchMetric() throws Exception {

        // Open the calendar app.
        assertTrue(mAppStartupHelper.startCollecting());
        mHelper.get().open();
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        mHelper.get().exit();
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, CALENDAR_PKG_NAME));

        // Open settings app
        HelperTestUtility.launchPackageViaAdb(SETTINGS_PKG_NAME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        HelperTestUtility.sendKeyCode(KEYCODE_HOME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, SETTINGS_PKG_NAME));
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);

        Map<String, StringBuilder> appLaunchMetrics = mAppStartupHelper.getMetrics();
        String coldLaunchCalendarMetricKey = String.format(COLD_LAUNCH_KEY_TEMPLATE,
                CALENDAR_PKG_NAME);
        String coldLaunchSettingsMetricKey = String.format(COLD_LAUNCH_KEY_TEMPLATE,
                SETTINGS_PKG_NAME);
        String coldLaunchCalendarCountPkgKey = String.format(COLD_LAUNCH_COUNT_PKG_KEY_TEMPLATE,
                CALENDAR_PKG_NAME);
        String coldLaunchSettingsCountPkgKey = String.format(COLD_LAUNCH_COUNT_PKG_KEY_TEMPLATE,
                CALENDAR_PKG_NAME);
        assertTrue(appLaunchMetrics.keySet().contains(coldLaunchCalendarMetricKey));
        assertTrue(appLaunchMetrics.keySet().contains(coldLaunchSettingsMetricKey));
        assertEquals(1,
                appLaunchMetrics.get(coldLaunchCalendarMetricKey).toString().split(",").length);
        assertEquals(1,
                appLaunchMetrics.get(coldLaunchSettingsCountPkgKey).toString().split(",").length);
        assertEquals(1,
                Integer.parseInt(appLaunchMetrics.get(coldLaunchCalendarCountPkgKey).toString()));
        assertEquals(1,
                Integer.parseInt(appLaunchMetrics.get(coldLaunchSettingsCountPkgKey).toString()));
        assertEquals(2, Integer.parseInt(appLaunchMetrics.get(COLD_LAUNCH_TOTAL_COUNT_KEY_TEMPLATE)
                .toString()));
        assertTrue(mAppStartupHelper.stopCollecting());

    }

    /**
     * Test warm launch metric.
     */
    @Test
    public void testWarmLaunchMetric() throws Exception {
        // Launch the app once and exit it so it resides in memory.
        mHelper.get().open();
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        // Press home and clear the cache explicitly.
        HelperTestUtility.sendKeyCode(KEYCODE_HOME);
        HelperTestUtility.clearCache();
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        // Start the collection here to test warm launch.
        assertTrue(mAppStartupHelper.startCollecting());
        // Launch the app; a warm launch occurs.
        mHelper.get().open();
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        Map<String, StringBuilder> appLaunchMetrics = mAppStartupHelper.getMetrics();
        String calendarWarmLaunchKey = String.format(WARM_LAUNCH_KEY_TEMPLATE, CALENDAR_PKG_NAME);
        assertTrue(appLaunchMetrics.keySet().contains(calendarWarmLaunchKey));
        assertEquals(1, appLaunchMetrics.get(calendarWarmLaunchKey).toString().split(",").length);
        assertTrue(mAppStartupHelper.stopCollecting());
        mHelper.get().exit();
    }

    /**
     * Test hot launch metric on settings, which is lightweight enough to trigger a hot launch.
     */
    @Test
    public void testHotLaunchMetric() throws Exception {
        // Launch the app once and go home so the app resides in memory.
        HelperTestUtility.launchPackageViaAdb(SETTINGS_PKG_NAME);
        HelperTestUtility.sendKeyCode(KEYCODE_HOME);
        // Start the collection here to test hot launch.
        assertTrue(mAppStartupHelper.startCollecting());
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        // Launch the app; a hot launch occurs.
        HelperTestUtility.launchPackageViaAdb(SETTINGS_PKG_NAME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        Map<String, StringBuilder> appLaunchMetrics = mAppStartupHelper.getMetrics();
        String calculatoHotLaunchKey = String.format(HOT_LAUNCH_KEY_TEMPLATE, SETTINGS_PKG_NAME);
        assertTrue(appLaunchMetrics.keySet().contains(calculatoHotLaunchKey));
        assertEquals(1, appLaunchMetrics.get(calculatoHotLaunchKey).toString().split(",").length);
        assertTrue(mAppStartupHelper.stopCollecting());
        HelperTestUtility.sendKeyCode(KEYCODE_HOME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, SETTINGS_PKG_NAME));
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
    }

    /**
     * Test that app startup fully drawn metric is collected for a single launch.
     */
    @Test
    public void testSingleLaunchStartupFullyDrawnMetric() throws Exception {
        // The Settings app is used here as it calls reportFullyDrawn(), which is required for the
        // AppStartFullyDrawn metric to be collected.
        // Start metric collection and then launch the Settings app.
        assertTrue(mAppStartupHelper.startCollecting());
        HelperTestUtility.launchPackageViaAdb(SETTINGS_PKG_NAME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        // Check that the collected metrics contains the key for the AppStartFullyDrawn metric.
        boolean hasFullyDrawnKey = false;
        Map<String, StringBuilder> appLaunchMetrics = mAppStartupHelper.getMetrics();
        for (String key : appLaunchMetrics.keySet()) {
            if (key.contains(FULLY_DRAWN_KEY_KEYWORD) && key.contains(SETTINGS_PKG_NAME)) {
                hasFullyDrawnKey = true;
            }
        }
        assertTrue(hasFullyDrawnKey);
        assertTrue(mAppStartupHelper.stopCollecting());
        HelperTestUtility.sendKeyCode(KEYCODE_HOME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, SETTINGS_PKG_NAME));
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
    }

    /**
     * Test that app startup fully drawn metric is collected for a single launch.
     */
    @Test
    public void testMultipleLaunchStartupFullyDrawnMetric() throws Exception {
        // The Settings app is used here as it calls reportFullyDrawn(), which is required for the
        // AppStartFullyDrawn metric to be collected.
        // Start metric collection and then cold launch the Settings app twice, as in this app
        // reportFullyDrawn() is only called during cold launch (the calling function is only called
        // during onCreate()).
        assertTrue(mAppStartupHelper.startCollecting());
        // 1st launch and kill.
        HelperTestUtility.launchPackageViaAdb(SETTINGS_PKG_NAME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        HelperTestUtility.sendKeyCode(KEYCODE_HOME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, SETTINGS_PKG_NAME));
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        // 2nd launch.
        HelperTestUtility.launchPackageViaAdb(SETTINGS_PKG_NAME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        // Check that the collected metrics contains the key for the AppStartFullyDrawn metric,
        // and that there are two values under this key.
        boolean hasFullyDrawnKey = false;
        Map<String, StringBuilder> appLaunchMetrics = mAppStartupHelper.getMetrics();
        for (String key : appLaunchMetrics.keySet()) {
            if (key.contains(FULLY_DRAWN_KEY_KEYWORD) && key.contains(SETTINGS_PKG_NAME)) {
                hasFullyDrawnKey = true;
                // There should be two values under this key.
                assertEquals(2, appLaunchMetrics.get(key).toString().split(",").length);
            }
        }
        assertTrue(hasFullyDrawnKey);
        assertTrue(mAppStartupHelper.stopCollecting());
        HelperTestUtility.sendKeyCode(KEYCODE_HOME);
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
        HelperTestUtility.clearApp(String.format(KILL_TEST_APP_CMD_TEMPLATE, SETTINGS_PKG_NAME));
        SystemClock.sleep(HelperTestUtility.ACTION_DELAY);
    }
}
