package org.darwin.denizenAdditions.commands;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncCommand extends BracedCommand implements Holdable {

    private static final AtomicLong ID_COUNTER = new AtomicLong(System.nanoTime());

    public AsyncCommand() {
        setName("async");
        setSyntax("async [<commands>]");
        setRequiredArguments(0, 1);
        isProcedural = true;
    }

    // <--[command]
    // @Name Async
    // @Syntax async [<commands>]
    // @Required 0
    // @Maximum 1
    // @Short Runs commands asynchronously (in a separate thread).
    // @Group queue
    // @Warning Experimental! Use only with commands known to be thread-safe.
    // @Description
    // Runs all commands inside the following block asynchronously in a worker thread.
    // The block inherits definitions and context from the current queue.
    // If the calling entry used ~waitable, the parent entry will wait until the async block finishes.
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        List<BracedData> bdat = getBracedCommands(scriptEntry);
        if (bdat != null && !bdat.isEmpty()) {
            scriptEntry.addObject("braces", bdat);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(ScriptEntry scriptEntry) {
        List<BracedData> braces = (List<BracedData>) scriptEntry.getObject("braces");
        if (braces == null || braces.isEmpty()) {
            Debug.echoError(scriptEntry, "Async command requires a following block of commands.");
            scriptEntry.setFinished(true);
            return;
        }

        List<ScriptEntry> innerEntries = braces.getFirst().value;

        String queueId = "ASYNC_BLOCK_" + Long.toHexString(ID_COUNTER.getAndIncrement());
        InstantQueue queue = new InstantQueue(queueId);

        List<ScriptEntry> cleanedEntries = new ArrayList<>();
        for (ScriptEntry entry : innerEntries) {
            ScriptEntry newEntry = entry.clone();
            newEntry.queue = queue;
            newEntry.entryData = (scriptEntry.entryData == null)
                    ? DenizenCore.implementation.getEmptyScriptEntryData()
                    : scriptEntry.entryData.clone();
            newEntry.entryData.scriptEntry = newEntry;
            newEntry.updateContext();
            cleanedEntries.add(newEntry);
        }
        queue.addEntries(cleanedEntries);

        ScriptQueue parentQueue = scriptEntry.getResidingQueue();
        if (parentQueue != null) {
            queue.getAllDefinitions().putAll(parentQueue.getAllDefinitions());
            queue.setContextSource(parentQueue.contextSource);
        }

        if (scriptEntry.shouldWaitFor()) {
            queue.callBack(() -> DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true)));
        } else {
            scriptEntry.setFinished(true);
        }

        scriptEntry.addObject("created_queue", new QueueTag(queue));

        DenizenCore.runAsync(() -> {
            try {
                queue.start(true);
            } catch (Throwable ex) {
                Debug.echoError("Error in async block: " + ex.getMessage());
                Debug.echoError(ex);
                if (scriptEntry.shouldWaitFor()) {
                    DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                }
            }
        });
    }
}
