package com.github.ilpersi.BHBot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * A hack-of-a BufferedReader which does NOT block indefinately when reading.
 * Original version is available here: https://community.oracle.com/message/5318833#5318833
 */
public class InputThread
{
    // NOTE: LinkedBlockingQueue is threadsafe!
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();

    // we done reading the input stream on the background thread?
    private boolean closed = false;

    Thread backgroundReaderThread;
    BHBotLogger logger;

    public InputThread(InputStream input, BHBotLogger logger) {
        this(new InputStreamReader(input));
        this.logger = logger;
    }

    public InputThread(final Reader reader) {
        backgroundReaderThread = new Thread(() -> {
            try (BufferedReader bfrdReader = (reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader))) {
                while (!Thread.interrupted()) {
                    String line = bfrdReader.readLine();
                    if (line == null) {
                        break;
                    }
                    lines.add(line);

                    BHBot.logger.trace("Input Thread Sleeping");
                    Misc.sleep(500);
                }
            } catch (IOException e) {
                logger.warn("Error while reading from standard input: ", e);
            } finally {
                closed = true;
                //throw new RuntimeIOException(e);
            }
        });
        // The JVM exits when the only running threads are all daemons.
        backgroundReaderThread.setDaemon(true);
        backgroundReaderThread.start();
    }

    /**
     * Returns the next line, blocking for 500 milliseconds for input then returns null.
     * If the background reader thread is interrupted then returns null.
     */
    public String readLine() throws IOException {
        try {
            return closed && lines.size()==0 ? null : lines.poll(500L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IOException("The BackgroundReaderThread was interrupted!");
        }
    }

    /**
     * Closes this reader (by interrupting the background reader thread).
     */
    public void close() {
        if( backgroundReaderThread!=null ){
            backgroundReaderThread.interrupt();
            backgroundReaderThread = null;
        }
    }
}