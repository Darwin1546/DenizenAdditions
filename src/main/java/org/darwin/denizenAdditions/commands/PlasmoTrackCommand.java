package org.darwin.denizenAdditions.commands;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import su.plo.voice.discs.DiscsPlugin;
import su.plo.voice.discs.PlasmoAudioPlayerManager;
import su.plo.voice.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import su.plo.voice.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

public class PlasmoTrackCommand extends AbstractCommand implements Holdable {

    public PlasmoTrackCommand() {
        setName("plasmotrack");
        setSyntax("plasmotrack [<url>]");
        setRequiredArguments(1, 1);
        autoCompile();
    }

    // <--[command]
    // @Name plasmotrack
    // @Syntax plasmotrack [<url>]
    // @Required 1
    // @Short Fetches track info from pv-addon-discs.
    // @Group external
    //
    // @Description
    // Retrieves the title and final playback URL of a track using pv-addon-discs audio manager.
    //
    // @Tags
    // <entry[saveName].name> returns the track title.
    // <entry[saveName].new_url> returns the resolved track URL.
    // <entry[saveName].failed> returns whether the track resolving has failed.
    // -->
    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("url") @ArgLinear String url) {

        Debug.echoDebug(scriptEntry, "Attempting to resolve track via pv-addon-discs: " + url);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("pv-addon-discs");
        if (plugin == null) {
            Debug.echoError(scriptEntry, "pv-addon-discs plugin not found!");
            scriptEntry.setFinished(true);
            return;
        }

        if (!(plugin instanceof DiscsPlugin) && !plugin.getClass().getName().contains("DiscsPlugin")) {
            Debug.echoDebug(scriptEntry, "pv-addon-discs loaded but plugin class is: " + plugin.getClass().getName());
        }

        final PlasmoAudioPlayerManager manager;
        try {
            manager = findAudioManager(plugin);
        } catch (Exception e) {
            Debug.echoError(scriptEntry, "Failed to obtain PlasmoAudioPlayerManager from pv-addon-discs: " + e);
            scriptEntry.setFinished(true);
            return;
        }

        if (manager == null) {
            Debug.echoError(scriptEntry, "PlasmoAudioPlayerManager is null (pv-addon-discs may not be fully initialized).");
            scriptEntry.setFinished(true);
            return;
        }

        CompletableFuture<AudioTrack> future;
        try {
            future = manager.getTrack(url);
        } catch (Throwable t) {
            Debug.echoError(scriptEntry, "Exception calling getTrack: " + t);
            scriptEntry.setFinished(true);
            return;
        }

        future.whenComplete((track, error) -> {
            if (error != null || track == null) {
                scriptEntry.saveObject("failed", new ElementTag(true));
                scriptEntry.setFinished(true);
                return;
            }

            AudioTrackInfo info = track.getInfo();
            scriptEntry.saveObject("name", new ElementTag(info.title != null ? info.title : ""));
            scriptEntry.saveObject("new_url", new ElementTag(info.uri != null ? info.uri : url));
            scriptEntry.saveObject("failed", new ElementTag(false));

            Debug.echoDebug(scriptEntry, "Loaded track: " + info.title + " (" + info.uri + ")");
            scriptEntry.setFinished(true);
        });
    }

    private static PlasmoAudioPlayerManager findAudioManager(Plugin plugin) throws ReflectiveOperationException {
        Class<?> cls = plugin.getClass();

        for (Field f : cls.getDeclaredFields()) {
            Class<?> t = f.getType();
            if (PlasmoAudioPlayerManager.class.isAssignableFrom(t)) {
                f.setAccessible(true);
                Object val = f.get(plugin);
                return (PlasmoAudioPlayerManager) val;
            }
        }

        String[] tryNames = new String[]{"audioPlayerManager", "audioManager", "audioPlayer", "playerManager"};
        for (String name : tryNames) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                Object val = f.get(plugin);
                if (val instanceof PlasmoAudioPlayerManager) {
                    return (PlasmoAudioPlayerManager) val;
                }
            } catch (NoSuchFieldException ignored) { }
        }

        Class<?> s = cls.getSuperclass();
        while (s != null && s != Object.class) {
            for (Field f : s.getDeclaredFields()) {
                if (PlasmoAudioPlayerManager.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(plugin);
                    return (PlasmoAudioPlayerManager) val;
                }
            }
            s = s.getSuperclass();
        }

        throw new NoSuchFieldException("No field with type PlasmoAudioPlayerManager found in plugin class " + cls.getName());
    }
}
