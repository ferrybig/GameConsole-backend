/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Fernando
 */
public class Server {

    private Process process;
    private final File dir;
    private final List<String> arguments;
    private final byte[] buffer;
    private int writeIndex;
    private long totalBytes;
    private BufferedWriter out;
    public final Map<Setting, String> settings = new HashMap<>();

    private static final byte[] DEFAULT_BYTES;
    static {
        DEFAULT_BYTES = ("[GameConsole] Welcome to gameconsole.\n"
                + "[GameConsole] to start your server, \n"
                + "[GameConsole] press the start button at the top of the screen\n\n").getBytes();
    }
    
    public Server(File dir, List<String> arguments, int bufferSize) {
        this.dir = dir;
        this.arguments = arguments;
        this.buffer = new byte[bufferSize];
		System.arraycopy(DEFAULT_BYTES, 0, buffer, 0, DEFAULT_BYTES.length);
		this.writeIndex = (int) (this.totalBytes = DEFAULT_BYTES.length);
//        this.writeMask = i - 1;
//        this.writeShift = j;
    }

    private void writeBytes(byte[] source, int start, int end) {
		if(end <= start) {
			throw new IllegalArgumentException("end <= start");
		}
		int length = end - start;
		if(length > buffer.length) {
			writeBytes(source, start + buffer.length, end);
			return;
		}
        synchronized (this) {
            if (this.writeIndex + length >= buffer.length) {
                // overflow
                int overflow = this.writeIndex + length - buffer.length;
                if (length != overflow) {
                    System.arraycopy(source, start, buffer, writeIndex, length - overflow);
                }
                System.arraycopy(source, start + overflow, buffer, 0, overflow);
                writeIndex = overflow;
            } else {
                System.arraycopy(source, start, buffer, writeIndex, length);
                writeIndex += length;
            }
            totalBytes += length;
            this.notifyAll();
        }
    }

    public long getCurrentWriteIndex() {
        return this.totalBytes;
    }

    public int getBufferSize() {
        return this.buffer.length;
    }

    public synchronized int readBytesBlocking(byte[] target, int targetStart, int targetLength, long readIndex, int maxAttempts) throws InterruptedException {
        int r = readBytes0(target, targetStart, targetLength, readIndex);
        int attempt = 0;
        while (r == 0 && attempt++ < maxAttempts) {
            if (r == 0) {
                this.wait(1000);
            }
            r = readBytes0(target, targetStart, targetLength, readIndex);
        }
        return r;
    }

    private int readBytes0(byte[] target, int targetStart, int targetLength, long readIndex) {
        if (readIndex < getLowestValidReadIndex()) {
            return -1;
        }
        long startPos = readIndex;
        long endPos = Math.min(this.totalBytes, startPos + targetLength);
        long total = endPos - startPos;
        if (total < 0) {
            return -1;
        }
        if (total == 0) {
            return 0;
        }
        int startRelative = (int) (startPos % this.getBufferSize());
        int endRelative = (int) (endPos % this.getBufferSize());

        if (endRelative < startRelative) {
            // Overflowing array index
            int bytesBeforeOverflow = this.getBufferSize() - startRelative;
            int bytesAfterOverflow = (int) (total - bytesBeforeOverflow);
            if (bytesBeforeOverflow != 0) {
                System.arraycopy(this.buffer, startRelative, target, targetStart, bytesBeforeOverflow);
            }
            if (bytesAfterOverflow != 0) {
                System.arraycopy(this.buffer, 0, target, targetStart + bytesBeforeOverflow, bytesAfterOverflow);
            }
        } else {
            System.arraycopy(this.buffer, startRelative, target, targetStart, (int) total);
        }
        return (int) total;
    }

    public int readBytes(byte[] buff, int start, int length, long readIndex) {
        if (readIndex < getLowestValidReadIndex()) {
            return -1;
        }
        return readBytes0(buff, start, length, readIndex);
    }

    public long getLowestValidReadIndex() {
        return Math.max(0, this.getCurrentWriteIndex() - this.getBufferSize() + 1);
    }

    public void start() {
        if (isRunning()) {
            return;
        }
        ProcessBuilder builder = new ProcessBuilder(arguments);
        builder.directory(dir);
        builder.redirectErrorStream(true);
        builder.redirectInput(ProcessBuilder.Redirect.PIPE);
        builder.redirectOutput(ProcessBuilder.Redirect.PIPE);

        try {
            process = null;
            
            process = builder.start();
            byte[] message = "[GameConsole] Server has started\n".getBytes();
            writeBytes(message, 0, message.length);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    byte[] buff = new byte[1024 * 2];
                    try (InputStream in = new BufferedInputStream(process.getInputStream())) {
                        int length;
						int offset = 0;
                        while ((length = in.read(buff, offset, buff.length - offset)) >= 0) {
							writeBytes(buff, 0, length);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        synchronized (Server.this) {
                            if (process != null && process.isAlive()) {
                                try {
                                    process.getOutputStream().close();
                                } catch (IOException ex) {
                                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                process.destroyForcibly();
                            }
                            process = null;
                            out = null;
                            Logger.getLogger(Server.class.getName()).info("Server has quit!");
                            byte[] message = "[GameConsole] Server has quit\n".getBytes();
                            writeBytes(message, 0, message.length);
                        }
                    }
                }
            }).start();
            this.out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        } catch (IOException ex) {
            byte[] e = ("[GameConsole] Error while starting server:" + ex.getMessage() + "\n").getBytes();
            this.writeBytes(e, 0, e.length);
        }

    }

    public void stop() {
        Logger.getLogger(Server.class.getName()).info("Stopping server");
        if (!isRunning()) {
            throw new IllegalStateException("Already stopped");
        }
        byte[] message = "[GameConsole] Destroying server\n".getBytes();
        writeBytes(message, 0, message.length);
        process.destroy();
    }

    public void forceStop() {
        Logger.getLogger(Server.class.getName()).info("Force stopping server");
        if (!isRunning()) {
            throw new IllegalStateException("Already stopped");
        }
        byte[] message = "[GameConsole] Force destroying server\n".getBytes();
        writeBytes(message, 0, message.length);
        process.destroyForcibly();
    }

    public void sendCommand(String command) throws IOException {
        if (!isRunning()) {
            return;
        }
        Logger.getLogger(Server.class.getName()).log(Level.INFO, "Sending command: {0}", command);
        byte[] message = ("[GameConsole] Sending command: "+command+"\n").getBytes();
        writeBytes(message, 0, message.length);
        out.append(command);
        out.newLine();
        out.flush();
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }
}
