package org.darwin.denizenAdditions.mechanisms;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.inventory.SlotHelper;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.entity.Player;

public class PlayerDropSlot {

    public static void register() {

        // <--[mechanism]
        // @object PlayerTag
        // @name drop_slot
        // @input ElementTag(Inventory slot)
        // @plugin Paper, DenizenAdditions
        // @description
        // Forces the player to drop item from inventory slot.
        // The slot argument can be any valid slot, see <@link language Slot Inputs>.
        // -->
        PlayerTag.tagProcessor.registerMechanism("drop_slot", false, ElementTag.class, (prop, mechanism, slot) -> {
            Player player = prop.getPlayerEntity();
            player.dropItem(SlotHelper.nameToIndexFor(slot.toString(), player.getInventory().getHolder()));
        });

    }
}
