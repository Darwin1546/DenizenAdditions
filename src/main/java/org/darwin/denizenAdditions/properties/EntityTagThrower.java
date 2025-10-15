package org.darwin.denizenAdditions.properties;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.objects.properties.entity.EntityProperty;
import com.denizenscript.denizencore.objects.Mechanism;
import org.bukkit.entity.Item;

import java.util.UUID;

public class EntityTagThrower extends EntityProperty<PlayerTag> {

    // <--[property]
    // @object EntityTag
    // @name thrower
    // @returns PlayerTag
    // @plugin DenizenAdditions
    // @description
    // Controls the thrower of the dropped item entity.
    // Returns an PlayerTag or null if none is set.
    // -->

    public static boolean describes(EntityTag entity) {
        return entity.getBukkitEntity() instanceof Item;
    }

    @Override
    public PlayerTag getPropertyValue() {
        Item item = (Item) getEntity();
        UUID thrower = item.getThrower();
        if (thrower == null) {
            return null;
        }
        return new PlayerTag(thrower);
    }

    @Override
    public String getPropertyId() {
        return "thrower";
    }

    @Override
    public void setPropertyValue(PlayerTag param, Mechanism mechanism) {
        Item item = (Item) getEntity();
        item.setThrower(param.getUUID());
    }

    public static void register() {
        autoRegister("thrower", EntityTagThrower.class, PlayerTag.class, false);
    }
}
