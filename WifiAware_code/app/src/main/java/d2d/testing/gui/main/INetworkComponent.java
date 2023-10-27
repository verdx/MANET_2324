package d2d.testing.gui.main;

import androidx.fragment.app.FragmentActivity;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = {NetworkModule.class})
public interface INetworkComponent {
//    BasicViewModel provideNetworkViewModel();
    void inject(MainFragment main);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder fragmentActivity(FragmentActivity fragmentActivity);

        INetworkComponent build();
    }

}
