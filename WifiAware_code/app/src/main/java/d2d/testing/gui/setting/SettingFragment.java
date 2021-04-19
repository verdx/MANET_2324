package d2d.testing.gui.setting;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import d2d.testing.R;

public class SettingFragment extends PreferenceFragmentCompat{

    private static final String TAG = "SettingFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

}
