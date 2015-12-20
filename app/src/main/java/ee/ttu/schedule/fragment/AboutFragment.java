package ee.ttu.schedule.fragment;


import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.vadimstrukov.ttuschedule.R;

import ee.ttu.schedule.utils.Constants;

public class AboutFragment extends PreferenceFragment {
    public static final String TAG = "AboutFragment";
    private static final String BUILD_VERSION = "build_version";
    private static final String SELECTED_GROUP = "selected_group";
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.preferences);
        final Activity activity = getActivity();
        findPreference(SELECTED_GROUP).setSummary(PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext()).getString(Constants.GROUP, null));
        try {
            final PackageInfo packageInfo =
                    activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            findPreference(BUILD_VERSION).setSummary(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            findPreference(BUILD_VERSION).setSummary("?");
        }
    }
}
