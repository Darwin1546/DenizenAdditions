package org.darwin.denizenAdditions.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.core.TaskScriptContainer;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RunAsyncCommand extends AbstractCommand implements Holdable {

    public RunAsyncCommand() {
        setName("run_async");
        setSyntax("run_async [<script>] (path:<name>) (def:<element>|.../defmap:<map>/def.<name>:<value>)");
        setRequiredArguments(1, -1);
        isProcedural = true;
    }

    // <--[command]
    // @Name Run_async
    // @Syntax run_async [<script>] (path:<name>) (def:<element>|.../defmap:<map>/def.<name>:<value>) (id:<name>) (speed:<value>/instantly) (delay:<value>)
    // @Required 1
    // @Maximum -1
    // @Short Runs a script in a new async queue.
    // @Guide https://guide.denizenscript.com/guides/basics/run-options.html
    // @Group queue
    //
    // @Warning Experimental! Use with extreme caution!
    // @Warning Most commands are not compatible with async execution.
    //
    // @Description
    // Runs a script in a new async queue.
    //
    // You must specify a script object to run.
    //
    // Optionally, use the "path:" argument to choose a specific sub-path within a script.
    //
    // Optionally, use the "def:" argument to specify definition values to pass to the script,
    // the definitions will be named via the "definitions:" script key on the script being ran,
    // or numerically in order if that isn't specified (starting with <[1]>).
    //
    // Alternately, use "defmap:<map>" to specify definitions to pass as a MapTag, where the keys will be definition names and the values will of course be definition values.
    //
    // Alternately, use "def.<name>:<value>" to define one or more  named definitions individually.
    //
    // Optionally, use the "speed:" argument to specify the queue command-speed to run the target script at,
    // or use the "instantly" argument to use an instant speed (no command delay applied).
    // If neither argument is specified, the default queue speed applies (normally instant, refer to the config file).
    // Generally, prefer to set the "speed:" script key on the script to be ran, rather than using this argument.
    //
    // Optionally, use the "delay:" argument to specify a delay time before the script starts running.
    //
    // Optionally, specify the "id:" argument to choose a custom queue ID to be used.
    // If none is specified, a randomly generated one will be used. Generally, don't use this argument.
    //
    // The run command is ~waitable. Refer to <@link language ~waitable>.
    //
    // @Tags
    // <entry[saveName].created_queue> returns the queue that was started by the run command.
    //
    // @Usage
    // Use to run a task script named 'MyTask'.
    // - run_async MyTask
    //
    // @Usage
    // Use to run a task script named 'MyTask' that isn't normally instant, instantly.
    // - run_async MyTask instantly
    //
    // @Usage
    // Use to run a local subscript named 'alt_path'.
    // - run_async <script> path:alt_path
    //
    // @Usage
    // Use to run 'MyTask' and pass 3 definitions to it.
    // - run_async MyTask def:A|Second_Def|Taco
    //
    // @Usage
    // Use to run 'MyTask' and pass 3 named definitions to it.
    // - run_async MyTask def.count:5 def.type:Taco def.smell:Tasty
    //
    // @Usage
    // Use to run 'MyTask' and pass a list as a single definition.
    // - run_async MyTask def:<list_single[<list[a|big|list|here]>]>
    // # MyTask can then get the list back by doing:
    // - define mylist <[1]>
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("script")
                    && arg.matchesArgumentType(ScriptTag.class)) {
                scriptEntry.addObject("script", arg.asType(ScriptTag.class));
            }
            else if (arg.matchesPrefix("path")) {
                scriptEntry.addObject("path", arg.asElement());
            }
            else if (arg.matchesPrefix("defmap") && arg.matchesArgumentType(MapTag.class)) {
                scriptEntry.addObject("def_map", arg.asType(MapTag.class));
            }
            else if (arg.matchesPrefix("def") && arg.matchesArgumentType(ListTag.class)) {
                scriptEntry.addObject("definitions", arg.asType(ListTag.class));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("script")) {
            throw new InvalidArgumentsException("Must specify a script to run!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ScriptTag script = scriptEntry.getObjectTag("script");
        ElementTag path = scriptEntry.getElement("path");
        MapTag defMap = scriptEntry.getObjectTag("def_map");
        ListTag definitions = scriptEntry.getObjectTag("definitions");

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), script, path, defMap, definitions);
        }

        if (!(script.getContainer() instanceof TaskScriptContainer container)) {
            Debug.echoError(scriptEntry, "Script must be a task script!");
            return;
        }

        Consumer<ScriptQueue> configure = (queue) -> {
            if (defMap != null) {
                for (var val : defMap.entrySet()) {
                    queue.addDefinition(val.getKey().str, val.getValue());
                }
            }
            scriptEntry.saveObject("created_queue", new QueueTag(queue));
        };

        // Получаем любой активный плагин Denizen (или свой)
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Denizen");
        if (plugin == null) {
            Debug.echoError("Denizen plugin not found! Cannot schedule async task properly.");
            scriptEntry.setFinished(true);
            return;
        }

        // Асинхронное выполнение
        CompletableFuture.runAsync(() -> {
            try {
                ScriptQueue queue = ScriptUtilities.createAndStartQueue(
                        container,
                        path != null ? path.asString() : null,
                        scriptEntry.entryData,
                        null,
                        configure,
                        null,
                        null,
                        definitions,
                        scriptEntry);

                if (scriptEntry.shouldWaitFor() && queue != null) {
                    queue.callBack(() ->
                            Bukkit.getScheduler().runTask(plugin, () -> scriptEntry.setFinished(true))
                    );
                }
            } catch (Exception e) {
                Debug.echoError("Error in run_async: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
