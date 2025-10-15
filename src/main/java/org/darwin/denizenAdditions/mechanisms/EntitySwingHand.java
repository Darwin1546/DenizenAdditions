package org.darwin.denizenAdditions.mechanisms;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.inventory.SlotHelper;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

public class EntitySwingHand {

    public static void register() {

        // <--[mechanism]
        // @object PlayerTag
        // @name armswing
        // @input ElementTag
        // @plugin Paper, DenizenAdditions
        // @description
        // Forces the player to swing his hand.
        // Input is either HAND or OFF_HAND.
        // -->
        PlayerTag.tagProcessor.registerMechanism("armswing", false, ElementTag.class, (prop, mechanism, slot) -> {

            EquipmentSlot hand = slot.asEnum(EquipmentSlot.class);
            if ( hand == null || !hand.isHand()) {
                mechanism.echoError("Invalid equipment slot '" + slot + "' specified: must be HAND or OFF_HAND.");
                return;
            }
            prop.getPlayerEntity().swingHand(EquipmentSlot.valueOf(slot.asString().toUpperCase()));
        });

    }
}
