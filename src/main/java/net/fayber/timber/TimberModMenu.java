package net.fayber.timber;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

// modmenu integration: registers the Timber config screen so the options can
// be edited from the Mods screen in singleplayer. only loaded when ModMenu is
// present (client); dedicated servers never touch this class. when Cloth Config
// is installed the nicer Cloth Config screen is used; otherwise it falls back
// to the hand-rolled screen.
public class TimberModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return TimberClothScreen::create;
        }
        return TimberConfigScreen::new;
    }
}
