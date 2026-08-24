package net.fayber.timber;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu integration: registers the Timber config screen so the options can
 * be edited from the Mods screen in singleplayer. Only loaded when ModMenu is
 * present (client); dedicated servers never touch this class.
 */
public class TimberModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TimberConfigScreen::new;
    }
}
