package d2d.testing.gui.main;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import d2d.testing.R;
import dagger.Module;
import dagger.Provides;

@Module
public class NetworkModule {
    private static final String WIFI_AWARE = "WFA";
    String mNetwork;
    public static boolean showIpPreference = false;

    @Provides
    BasicViewModel provideNetworkViewModel(FragmentActivity activity) {
        readConfig(activity.getResources().openRawResource(R.raw.config));
        switch (mNetwork){
            case WIFI_AWARE:
                return new ViewModelProvider(activity).get(WifiAwareViewModel.class);
            default:
                showIpPreference = true;
                return new ViewModelProvider(activity).get(DefaultViewModel.class);
        }

    }

//    @Binds
//    abstract BasicViewModel bindBasicViewModel(BasicViewModel basicViewModel);



    private void readConfig(InputStream is){
        Pattern networkPattern = Pattern.compile("use-network:(\\w+)");

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String str = null;

        try {
            while ((str = br.readLine()) != null) {
                Matcher matcher = networkPattern.matcher(str);
                if (matcher.matches()) {
                    mNetwork = matcher.group(1); // Extract the network type value
                }
            }
            is.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
