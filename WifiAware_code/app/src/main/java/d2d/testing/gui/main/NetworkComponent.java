package d2d.testing.gui.main;

import androidx.fragment.app.FragmentActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {NetworkModule.class, FragmentActivityModule.class})
public interface NetworkComponent {
    BasicViewModel provideNetworkViewModel();
    void inject(MainFragment main);
}
