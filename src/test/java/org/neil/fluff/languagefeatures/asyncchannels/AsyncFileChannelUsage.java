package org.neil.fluff.languagefeatures.asyncchannels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * This class explores some of the usages of Java 7's AsynchronousFileChannel.
 */
public class AsyncFileChannelUsage {
    private static final Log LOG = LogFactory.getLog(AsyncFileChannelUsage.class);
    
    /** A JUnit rule to handle cleanup of my temporary test file. */
    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private Path testFile;
    
    @Before public void createTestFile() throws IOException {
        // Create a temporary 'large' file in which to seek about and read test data.
        File f = tempFolder.newFile(AsyncFileChannelUsage.class.getSimpleName() + "_test.txt");
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) {
            final int filesize = 1024;
            // 100 chars and a newline
            for (int i = 0; i < filesize; i++) {
                for (int j = 0; j < 10; j++) {
                    writer.write("0123456789");
                }
                writer.write("\n");
            }
            writer.flush();
        }
        testFile = f.toPath();
    }
    
    @Test public void basicAsyncFileReadWithFuture() throws IOException, InterruptedException, ExecutionException {
        // We get an async file channel...
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(testFile, StandardOpenOption.READ);
        
        // ...set up a buffer into which we'll read the file contents (some of) ...
        final int bytesToRead = 10_000;
        
        ByteBuffer buffer = ByteBuffer.allocate(bytesToRead);
        // ... start the reading...
        Future<Integer> bytesRead = channel.read(buffer, 0);
        
        // ... and while it's running we could do other things.
        
        // Here, we've got nothing else to do, so we'll just block on the result
        int result = bytesRead.get();
        
        assertEquals(bytesToRead, result);
    }
    
    @Test public void basicAsyncFileReadWithCallback() throws IOException, InterruptedException, ExecutionException {
        // An alternate form of async read.
        
        // As before, we get an async file channel...
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(testFile, StandardOpenOption.READ);
        
        // ...set up a buffer into which we'll read the file contents (some of) ...
        final int bytesToRead = 10_000;
        
        ByteBuffer buffer = ByteBuffer.allocate(bytesToRead);
        
        // But this time we start the async read & provide a callback for onComplete...
        channel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>()
        {
            @Override public void completed(Integer result, ByteBuffer attachment)
            {
                assertEquals(bytesToRead, result.intValue());
            }
            
            @Override public void failed(Throwable exc, ByteBuffer attachment)
            {
                fail("Async file read failed.");
            }
        });
        
        // ... and while it's running we could do other things.
        
        // TODO But now we need to do a bit work to wait for the work to complete.
    }
}
