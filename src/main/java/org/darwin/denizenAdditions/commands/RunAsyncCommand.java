package org.darwin.denizenAdditions.commands;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

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
    // @Syntax run_async [<script>] (path:<name>) (def:<element>|.../defmap:<map>/def.<name>:<value>) (id:<name>) (delay:<value>)
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
        MapTag defMap = new MapTag();
        for (Argument arg : scriptEntry) {
            if (arg.matchesPrefix("i", "id")) {
                scriptEntry.addObject("id", arg.asElement());
            }
            else if (arg.matchesPrefix("def")) {
                scriptEntry.addObject("definitions", arg.asType(ListTag.class));
            }
            else if (arg.matchesPrefix("defmap")
                    && arg.matchesArgumentType(MapTag.class)) {
                defMap.putAll(arg.asType(MapTag.class));
            }
            else if (arg.matchesPrefix("delay")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("delay", arg.asType(DurationTag.class));
            }
            else if (arg.hasPrefix()
                    && arg.getPrefix().getRawValue().startsWith("def.")) {
                defMap.putObject(arg.getPrefix().getRawValue().substring("def.".length()), arg.object);
            }
            else if (!scriptEntry.hasObject("script")
                    && arg.matchesArgumentType(ScriptTag.class)
                    && arg.limitToOnlyPrefix("script")) {
                scriptEntry.addObject("script", arg.asType(ScriptTag.class));
            }
            else if (!scriptEntry.hasObject("path")
                    && arg.matchesPrefix("path", "p")) {
                scriptEntry.addObject("path", arg.asElement());
            }
            else if (!scriptEntry.hasObject("script") && !scriptEntry.hasObject("path")
                    && !arg.hasPrefix() && arg.asElement().asString().contains(".")) {
                String path = arg.asElement().asString();
                int dotIndex = path.indexOf('.');
                ScriptTag script = ScriptTag.valueOf(path.substring(0, dotIndex), CoreUtilities.noDebugContext);
                if (script == null) {
                    arg.reportUnhandled();
                }
                else {
                    scriptEntry.addObject("script", script);
                    scriptEntry.addObject("path", new ElementTag(path.substring(dotIndex + 1)));
                }
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("script")) {
            throw new InvalidArgumentsException("Must define a SCRIPT to be run.");
        }
        if (!defMap.isEmpty()) {
            scriptEntry.addObject("def_map", defMap);
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag pathElement = scriptEntry.getElement("path");
        ScriptTag script = scriptEntry.getObjectTag("script");
        MapTag defMap = scriptEntry.getObjectTag("def_map");
        DurationTag delay = scriptEntry.getObjectTag("delay");
        ListTag definitions = scriptEntry.getObjectTag("definitions");
        String path = pathElement != null ? pathElement.asString() : null;

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), script, pathElement, defMap, definitions);
        }

        if (path != null && !script.getContainer().containsScriptSection(path)) {
            Debug.echoError(scriptEntry, "Script run failed (invalid path)!");
            return;
        }

        Consumer<ScriptQueue> configure = (queue) -> {
            if (delay != null) {
                queue.delayUntil(DenizenCore.serverTimeMillis + delay.getMillis());
            }
            if (defMap != null) {
                for (var val : defMap.entrySet()) {
                    queue.addDefinition(val.getKey().str, val.getValue());
                }
            }
            scriptEntry.saveObject("created_queue", new QueueTag(queue));
            queue.procedural = scriptEntry.getResidingQueue().procedural;
        };

        // Асинхронное выполнение
        DenizenCore.runAsync(() -> {
            try {
                ScriptQueue queue = ScriptUtilities.createAndStartQueue(
                        script.getContainer(),
                        path,
                        scriptEntry.entryData,
                        null,
                        configure,
                        null,
                        null,
                        definitions,
                        scriptEntry);
            } catch (Exception e) {
                Debug.echoError(scriptEntry, e);
            }
            finally {
                if (scriptEntry.shouldWaitFor()) {
                    DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                }
            }
        });
    }
}
