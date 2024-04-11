package de.swiftbird.elasticandroid;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.click;

/**
 * Extends the basic sanity UI test to interact with buttons that start new activities.
 * This test demonstrates clicking buttons in the main activity and verifying
 * that new activities containing specific text are launched accordingly and no crashes occur.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppInteractionTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Test to verify interactions with buttons in the main activity.
     * Each button press is expected to launch a new activity. The presence of specific
     * text within those activities confirms the expected navigation occurred.
     */
    @Test
    public void testButtonInteractionsAndActivityLaunch() {
        // Click on the Accept button in the dialog and verify it is displayed
        onView(withText("ACCEPT")).inRoot(isDialog()).check(matches(isDisplayed())).perform(click());

        // Click on the Help button and verify the Help activity is shown
        onView(withId(R.id.btnHelp)).perform(click());
        onView(withText("Version and Legal")).check(matches(isDisplayed()));
        pressBack(); // Navigate back to the MainActivity

        // Click on the License button and verify the License activity is shown
        onView(withId(R.id.btnLicenses)).perform(click());

        // License is long so just look for tvLicenseText
        onView(withId(R.id.tvLicenseText)).check(matches(isDisplayed()));

        // Click on the Open Source Licenses button and verify the OssLicensesMenuActivity is shown
        onView(withId(R.id.btnOSSLicenses)).perform(click());
        // onView(withId(R.id.act)).check(matches(withText("Open Source Licenses")); TODO: Fix this

        pressBack(); // Navigate back to the LicenseActivity
        pressBack(); // Navigate back to the MainActivity

        // Click on the Legal button and verify the Legal activity is shown
        onView(withId(R.id.btnLegal)).perform(click());

        // Legal is long so just look for legalDisclaimerText
        onView(withId(R.id.tvLegalText)).check(matches(isDisplayed()));

        // No need to pressBack() if this is the last activity interaction in the test
    }
}
