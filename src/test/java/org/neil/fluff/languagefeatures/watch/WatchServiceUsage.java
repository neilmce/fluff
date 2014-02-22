package org.neil.fluff.languagefeatures.watch;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * This class explores some of the usages of Java 7's WatchService.
 * <p/>
 * Some notes from running this on Mac OS X 10.7.5 with Java 1.7
 * <ul>
 * <li>You only receive events for changes directly within a watched folder, not at arbitrary depth.</li>
 * <li>It seems to take just under 10s (and occasionally just under 5s) to receive notification
 *     of a file creation in a watched folder. See {@link #watchEmptyFileCreation()}</li>
 * </ul>
 */
public class WatchServiceUsage {
    private static final Log LOG = LogFactory.getLog(WatchServiceUsage.class);

    @Rule public TestFolder testFilesFolders = new TestFolder();
    
    public final FileSystem fileSystem = FileSystems.getDefault();
    public WatchService     watcher;
    
    @Before public void initWatcher() throws IOException {
        watcher = fileSystem.newWatchService();
    }
    
    @After public void destroyWatcher() throws IOException {
        watcher.close();
    }
    
    @Test public void watchEmptyFileCreation() throws Exception {
        // Create a folder...
        final Path folder = createFolder("watchedFolder");
        
        // ...start watching it...
        watchFolder(folder);
        
        // ... create a file within that folder.
        final String fileName = "newFile.txt";
        createEmptyFile(folder, fileName);
        
        // ...we'll check how long it takes for the notification to come back.
        final long fileCreationTime = System.currentTimeMillis();
        
        // ... take the next event (blocking call)...
        WatchKey nextEvent = watcher.take();
        final long eventNotificationTime = System.currentTimeMillis();
        
        LOG.info("It took " + (eventNotificationTime - fileCreationTime) + " ms for the file-create event to appear.");
        
        // Now let's look in more detail at what's in the event.
        List<WatchEvent<?>> events = nextEvent.pollEvents();
        
        // There's only one event
        assertEquals(1, events.size());
        WatchEvent<?> event = events.get(0);
        
        // There should only have been one create event. (multiple similar events e.g. modifications can build up)
        assertEquals(1, event.count());
        
        assertEquals(Path.class,     event.kind().type());
        assertEquals("ENTRY_CREATE", event.kind().name());
        
        // The context we're given is the Path, in this case relative to the watched folder.
        assertEquals(Paths.get(fileName) ,event.context());
    }
    
    // TODO modification (content) in a folder
    // TODO rename in a folder
    // TODO delete in a folder
    // TODO modification (rename) of watched folder
    // TODO deletion of watched folder
    // TODO multiple creates in watched folder
    // TODO multiple deletions in watched folder
    // TODO multiple renames in watched folder
    // TODO multiple modifications in watched folder
    // TODO fast create and delete in watched folder
    // TODO fast delete and create file with same name in watched folder
    // TODO fast chain of renames in watched folder
    // TODO fast chain of conflicting renames in watched folder
    // TODO move out of watched folder
    // TODO move into watched folder
    // TODO create non-empty file
    // TODO slowly append new content to file
    
    private WatchKey watchFolder(final Path folder) throws IOException {
        LOG.debug("Adding watch to folder " + folder);
        return folder.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
    }
    
    private Path createEmptyFile(Path parentFolder, String fileName) throws IOException {
        final Path file = testFilesFolders.newEmptyFile(parentFolder, fileName);
        
        LOG.debug("Created empty file: " + file);
        
        return file;
    }
    
    /** Creates a folder in the Java temporary folder, giving it a random name. */
    private Path createFolder(String folderName) throws IOException {
        File folder = testFilesFolders.newFolder(folderName);
        
        LOG.debug("Creating folder: " + folder.toPath());
        
        return folder.toPath();
    }
    
    /** Creates a folder under the specified parent folder, giving it the specified name. */
    private Path createFolder(Path parentFolder, String folderName) throws IOException {
        return testFilesFolders.newFolder(parentFolder, folderName);
    }
    
    
    
    /** A JUnit Rule with some more helpful methods than the rather basic TemporaryFolder. */
    private static class TestFolder extends TemporaryFolder {
        
        /** Creates a new folder under the specified parent folder with the specified name. */
        public Path newFolder(Path parentFolder, String folderName) {
            // JUnit's TemporaryFolder rule expects us to provide a String[] with folder names,
            // starting underneath the temporary folder.
            final Path tempFolder = getRoot().toPath();
            
            // So let's make the provided parentFolder a path relative to the temporary folder.
            final Path parentFolderRelativeToTmp = tempFolder.relativize(parentFolder);
            
            // And add all those elements to our array...
            final List<String> pathElements = new ArrayList<>();
            for (Iterator<Path> iter = parentFolderRelativeToTmp.iterator(); iter.hasNext(); ) {
                pathElements.add(iter.next().getFileName().toString());
            }
            // ...along with the new name
            pathElements.add(folderName);
            
            // and then create the new folder.
            File newFolder = this.newFolder(pathElements.toArray(new String[0]));
            
            return newFolder.toPath();
        }
        
        public Path newEmptyFile(Path parentFolder, String fileName) throws IOException
        {
            Path filePath = parentFolder.resolve(Paths.get(fileName));
            return Files.createFile(filePath);
        }
    }
}
