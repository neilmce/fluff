package org.neil.fluff.languagefeatures.watch;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * This class explores some of the usages of Java 7's WatchService.
 * <p/>
 * Some notes from running this on Mac OS X 10.7.5 with Java 1.7
 * <ul>
 * <li>You only receive events for changes directly within a watched folder, not at arbitrary depth.</li>
 * <li>I've watched 16000 folders and created one file in each of those and it behaved as expected and quickly.
 *     See {@link #watchLargeNumberOfFolders()}.</li>
 * <li>It seems to take just under 10s (and occasionally just under 5s) to receive notification
 *     of a single file creation in a watched folder. See {@link #watchEmptyFileCreation()}</li>
 * <li>A sequence of multiple modifications on a single file will only be reported as one
 *     modification. See {@link #watchModifiedFile()}.</li>
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
        final WatchKey watchKey = addWatch(folder);
        
        // ... create a file within that folder.
        final String fileName = "newFile.txt";
        createEmptyFile(folder, fileName);
        
        // ...we'll check how long it takes for the notification to come back.
        final long fileCreationTime = System.currentTimeMillis();
        
        // ... take the next event (blocking call)...
        WatchKey nextEvent = watcher.take();
        // note that we get the same WatchKey object as we got at registration.
        assertSame(watchKey, nextEvent);
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
    
    @Test public void watchNonEmptyFileCreation() throws Exception {
        // Create a folder...
        final Path folder = createFolder("watchedFolder");
        
        // ...start watching it...
        addWatch(folder);
        
        // ... create a file within that folder.
        final String fileName = "newFile.txt";
        createNonEmptyFile(folder, fileName);
        
        // ... take the next event (blocking call)...
        WatchKey nextEvent = watcher.take();
        
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
    
    @Test public void watchModifiedFile() throws Exception {
        // Create a folder...
        final Path folder = createFolder("watchedFolder");
        
        // ... create a file within that folder.
        final String fileName = "newFile.txt";
        final Path file = createNonEmptyFile(folder, fileName);
        
        // ...start watching the folder...
        addWatch(folder, StandardWatchEventKinds.ENTRY_MODIFY);
        
        // Simply modifying the file at this point never produces a modification event on Mac OS X 10.7 with Java 7.
        // I wonder is this because file modification detection is based on filesystem polling? If it were, the system
        // would need to be polled twice to see any changes and the immediate modification of a file could predate the first
        // poll?
        //
        // In any case, we'll trigger a series of modifications and see how long the WatchService takes to react - if indeed
        // it does react - and then how many modification events it reports.
        final int modCount = 10;
        final CountDownLatch latch = new CountDownLatch(modCount);
        Runnable modifyFile = new Runnable() {
            @Override public void run() {
                for (int i = 0; i < modCount; i++) {
                    try {
                        modifyFile(file, "modified file " + i);
                        latch.countDown();
                    } catch (IOException ignored) {
                        // Intentionally empty
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.interrupted();
                    }
                }
            }
        };
        new Thread(modifyFile).start();
        latch.await();
        
        // ... take the next event (blocking call)...
        WatchKey nextEvent = watcher.take();
        
        // Now let's look in more detail at what's in the event.
        List<WatchEvent<?>> events = nextEvent.pollEvents();
        
        // There's only one event
        assertEquals(1, events.size());
        WatchEvent<?> event = events.get(0);
        
        // Note that afte 10 modifications, only 1 has been detected. (file polling?)
        assertEquals(1, event.count());
        
        assertEquals(Path.class,     event.kind().type());
        assertEquals("ENTRY_MODIFY", event.kind().name());
        
        // The context we're given is the Path, in this case relative to the watched folder.
        assertEquals(Paths.get(fileName) ,event.context());
    }
    
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
    // TODO slowly append new content to file
    // TODO move file between two watched folders
    // TODO add multiple watches to the same folder - legal?
    // TODO link files
    // TODO hidden files
    // TODO file permissions - can user see changes on files they have no permission to see? Or rather: what happens?
    // TODO Load tests - large numbers of creates, deletes, modifications-to-single-file, modifications-to-many-files - within one WatchKey.
    // TODO unwatching while events are pending?
    // TODO There's a watch kind of OVERFLOW. How do we hit it?
    
    /** This test method checks that it is possible to watch large numbers of folders. */
    @Ignore("Ignored because it generates a lot of logging, but this passes just fine.")
    @Test public void watchLargeNumberOfFolders() throws Exception {
        final int folderCount = 128;
        
        Map<Path, WatchKey> watchedFolders = new HashMap<>();
        
        // Create and watch all these folders.
        for (int i = 0; i < folderCount; i++) {
            final Path folder = createFolder("watchedFolder" + i);
            
            watchedFolders.put(folder, addWatch(folder));
        }
        
        // Then create an empty file in each of them.
        for (Path folder : watchedFolders.keySet()) {
            createEmptyFile(folder, "file.txt");
        }
        
        // How many single events did we receive from all these folders?
        int eventsReceived = 0;
        for (int i = 0; i < folderCount; i++) {
            WatchKey nextWatchKey = watcher.take();
            List<WatchEvent<?>> events = nextWatchKey.pollEvents();
            assertEquals(1, events.size());
            eventsReceived++;
        }
        
        // It should be one per folder.
        assertEquals(folderCount, eventsReceived);
    }
    
    private WatchKey addWatch(final Path path) throws IOException {
        LOG.debug("Adding watch to " + path);
        return path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
    }
    
    private WatchKey addWatch(final Path path, Kind<Path>... watchKinds) throws IOException {
        LOG.debug("Adding watch to " + path);
        return path.register(watcher, watchKinds);
    }
    
    private Path createEmptyFile(Path parentFolder, String fileName) throws IOException {
        final Path file = testFilesFolders.newEmptyFile(parentFolder, fileName);
        
        LOG.debug("Created empty file: " + file);
        
        return file;
    }
    
    private Path createNonEmptyFile(Path parentFolder, String fileName) throws IOException {
        Path file = createEmptyFile(parentFolder, fileName);
        
        modifyFile(file, "Newly created file.");
        
        return file;
    }
    
    private void modifyFile(Path file, String content) throws IOException {
        LOG.debug("Modifying content in file: " + file);
        try (BufferedWriter w = Files.newBufferedWriter(file, Charset.defaultCharset())) {
            w.append(content);
            w.newLine();
            w.flush();
        }
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
