package org.darwin.denizenAdditions;

import com.denizenscript.denizen.Denizen;
import org.bukkit.plugin.java.JavaPlugin;
import org.darwin.denizenAdditions.tags.BinaryTagEncrypt;

public final class DenizenAdditions extends JavaPlugin {

    @Override
    public void onEnable() {
        BinaryTagEncrypt.register();
    }

    @Override
    public void onDisable() {
        Denizen.getInstance().onDisable();
    }
}
