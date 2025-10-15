package org.darwin.denizenAdditions.properties;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.objects.properties.entity.EntityProperty;
import com.denizenscript.denizencore.objects.Mechanism;
import org.bukkit.entity.Item;

import java.util.UUID;

public class EntityTagOwner extends EntityProperty<PlayerTag> {

    // <--[property]
    // @object EntityTag
    // @name item_owner
    // @returns PlayerTag
    // @plugin DenizenAdditions
    // @description
    // Controls the owner of the dropped item entity. Only owner of the item will be able to pick it up.
    // Returns an PlayerTag or null if none is set.
    // -->

    public static boolean describes(EntityTag entity) {
        return entity.getBukkitEntity() instanceof Item;
    }

    @Override
    public PlayerTag getPropertyValue() {
        Item item = (Item) getEntity();
        UUID owner = item.getOwner();
        if (owner == null) {
            return null;
        }
        return new PlayerTag(owner);
    }

    @Override
    public String getPropertyId() {
        return "item_owner";
    }

    @Override
    public void setPropertyValue(PlayerTag param, Mechanism mechanism) {
        Item item = (Item) getEntity();
        item.setOwner(param.getUUID());
    }

    public static void register() {
        autoRegister("item_owner", EntityTagOwner.class, PlayerTag.class, false);
    }
}
