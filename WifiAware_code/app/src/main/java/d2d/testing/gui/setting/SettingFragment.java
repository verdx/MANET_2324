package d2d.testing.gui.setting;

import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import javax.inject.Inject;

import d2d.testing.R;
import d2d.testing.gui.main.NetworkModule;

public class SettingFragment extends PreferenceFragmentCompat{

    private static final String TAG = "SettingFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

    }

}
