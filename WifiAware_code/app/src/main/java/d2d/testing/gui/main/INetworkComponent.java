package d2d.testing.gui.main;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {NetworkModule.class, FragmentActivityModule.class})
public interface INetworkComponent {
//    BasicViewModel provideNetworkViewModel();
    void inject(MainFragment main);
}
