package org.darwin.denizenAdditions.tags;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.properties.item.ItemPotion;
import com.denizenscript.denizencore.objects.core.MapTag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EntityTagPotionEffect {

    public static void register() {

        // <--[tag]
        // @attribute <EntityTag.effect[<effect>]>
        // @returns MapTag
        // @plugin DenizenAdditions
        // @description
        // Returns the potion effect MapTag entity has.
        // Returns null if entity has no specified potion effect.
        // The effect type must be from <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html>.
        // -->
        EntityTag.tagProcessor.registerTag(MapTag.class, "effect", (attribute, entity) -> {

            if (!attribute.hasParam()) {
                attribute.echoError("The effect must be specified.");
                return null;
            }
            PotionEffectType effectType = PotionEffectType.getByName(attribute.getParam());
            //PotionEffectType effectType = Registry.MOB_EFFECT.get(NamespacedKey.minecraft(attribute.getParam().toLowerCase()));
            if (effectType == null) {
                attribute.echoError("Invalid effect type specified: " + attribute.getParam());
                return null;
            }
            PotionEffect effect = entity.getLivingEntity().getPotionEffect(effectType);
            if (effect == null) {
                attribute.echoError("The entity has no effect: " + attribute.getParam());
                return null;
            }

            return ItemPotion.effectToMap(effect, true);

        });

    }
}
