package org.darwin.denizenAdditions.tags;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.core.ListTag;

public class PlayerTagChannels {

    public static void register() {

        // <--[tag]
        // @attribute <EntityTag.client_channels>
        // @returns ListTag
        // @plugin Paper, DenizenAdditions
        // @description
        // Returns the list of plugin channels that this player is listening on.
        // -->
        PlayerTag.registerOnlineOnlyTag(ListTag.class, "client_channels", (attribute, player) -> {
            return new ListTag(player.getPlayerEntity().getListeningPluginChannels());
        });
    }
}
