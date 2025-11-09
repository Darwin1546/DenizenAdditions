package org.darwin.denizenAdditions;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.darwin.denizenAdditions.commands.PlasmoTrackCommand;
import org.darwin.denizenAdditions.mechanisms.EntitySwingHand;
import org.darwin.denizenAdditions.mechanisms.PlayerDropSlot;
import org.darwin.denizenAdditions.properties.EntityFreezeTickingLocked;
import org.darwin.denizenAdditions.properties.EntityTagOwner;
import org.darwin.denizenAdditions.properties.EntityTagThrower;
import org.darwin.denizenAdditions.tags.BinaryTagEncrypt;
import org.darwin.denizenAdditions.tags.EntityTagPotionEffect;
import org.darwin.denizenAdditions.tags.PlayerTagChannels;

public final class DenizenAdditions extends JavaPlugin {

    @Override
    public void onEnable() {
        BinaryTagEncrypt.register();
        PlayerTagChannels.register();
        EntityTagPotionEffect.register();
        PlayerDropSlot.register();
        EntitySwingHand.register();
        PropertyParser.registerProperty(EntityFreezeTickingLocked.class, EntityTag.class);
        PropertyParser.registerProperty(EntityTagThrower.class, EntityTag.class);
        PropertyParser.registerProperty(EntityTagOwner.class, EntityTag.class);
        if (Bukkit.getPluginManager().getPlugin("pv-addon-discs") != null) {
            DenizenCore.commandRegistry.registerCommand(PlasmoTrackCommand.class);
        }
    }

    @Override
    public void onDisable() {
        Denizen.getInstance().onDisable();
    }
}
