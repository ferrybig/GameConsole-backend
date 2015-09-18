/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Fernando
 */
public class GameConsole {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        HttpSnoopServer.main(args);
    }

    private final ThreadLocal<StringBuilder> strings = new ThreadLocal<StringBuilder>() {

        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };
    private final ThreadLocal<byte[]> clientbuff = new ThreadLocal<byte[]>() {

        @Override
        protected byte[] initialValue() {
            return new byte[1024 * 16];
        }
    };
    private Map<String, Server> servers = new HashMap<>();

    {
        Server s;
        servers.put("test", s = new Server(new File("server"), Arrays.asList("java", "-jar", "spigot-1.8.jar"), 1024 * 64));
        s.settings.put(new Setting("port", Setting.Type.PORT, 1, true), "25565");
        s.settings.put(new Setting("ip", Setting.Type.IP, 1, true), "192.168.178.89");
        s.settings.put(new Setting("server-name", Setting.Type.NORMAL_STRING, 1, false), "Unknown server");
        s.settings.put(new Setting("moth", Setting.Type.NORMAL_STRING, 1, true), "Minecraft-server");
    }

    public void handleRequest(
            OutputStream output, HttpHeaders clientheaders, Map<String, String> serverheaders,
            AtomicReference<HttpResponseStatus> status, String path, String post, ChannelHandlerContext ctx) throws IOException, InterruptedException {
        String contentTypeHeader = clientheaders.get("content-type");
        serverheaders.put("X-Robots-Tag", "noindex, nofollow");
        
        if (path.endsWith("service=config")) {
            JSONObject config = new JSONObject();
            config.put("authmethod", "password");

                //config.put("authmethod", "none");
                //config.put("authmethod", "url");
            //config.put("authmethodarg1", "");
            JSONArray endpoints = new JSONArray();
            endpoints.put("/service");
            config.put("endpoints", endpoints);
            byte[] s = config.toString().getBytes("UTF-8");
            output.write(s);

            status.set(HttpResponseStatus.OK);
            serverheaders.put("cache-control", "max-age=120");
            return;
        }
        if (contentTypeHeader == null || !contentTypeHeader.startsWith("application/json")) {
            status.set(HttpResponseStatus.BAD_REQUEST);
            return;
        }
        if (post.isEmpty()) {
            status.set(HttpResponseStatus.BAD_REQUEST);
            return;
        }
        JSONObject incoming = new JSONObject(post);
        
        
        
        if (path.endsWith("service=password")) {
            String username = incoming.optString("username", "");
            String password = incoming.optString("password", "");
            String random = incoming.optString("random", "");
            
            JSONObject auth = new JSONObject();
            if(username.isEmpty() || password.isEmpty() || random.isEmpty()) {
                auth.put("random", "THIS_MUST_BE_RANDOM_FOR_EVERY_USER");
            } else {
                if(username.equals("root") && password.equals("root")) {
                    auth.put("session_token", "token");
                } else {
                    auth.put("error", "Invalid username or password");
                }
            }
            byte[] s = auth.toString().getBytes("UTF-8");
            output.write(s);

            status.set(HttpResponseStatus.OK);
            return;
        }
        
        
        
        String target = incoming.optString("target", "server");
        String action = incoming.optString("action", "");
        StringBuilder b = this.strings.get();
        b.setLength(0);

        switch (target) {
            case "server": {
                String server = incoming.optString("server", "");
                Logger.getLogger(GameConsole.class.getName()).log(Level.INFO, "Request: {0}: {1}: {2}", new Object[]{target, action, server});

                if (!this.servers.containsKey(server)) {
                    status.set(HttpResponseStatus.NOT_FOUND);
                    return;
                }
                final Server s = this.servers.get(server);

                switch (action) {
                    case "start": {
                        synchronized (s) {
                            JSONObject obj = new JSONObject();
                            if (!s.isRunning()) {
                                s.start();
                                obj.put("stage-changed", "now");
                            }

                            obj.put("state", s.isRunning() ? "started" : "stopped");
                            obj.put("server-id", server);
                            obj.put("readIndex", s.getCurrentWriteIndex());
                            obj.put("bufSize", s.getBufferSize());
                            b.append(obj.toString());
                        }
                    }
                    break;
                    case "stop": {
                        synchronized (s) {
                            if (s.isRunning()) {
                                s.stop();
                            }
                            JSONObject obj = new JSONObject();
                            obj.put("state", s.isRunning() ? "started" : "stopped");
                            obj.put("server-id", server);
                            obj.put("readIndex", s.getCurrentWriteIndex());
                            obj.put("bufSize", s.getBufferSize());
                            b.append(obj.toString());
                        }
                    }
                    break;
                    case "settings": {
                        synchronized (s) {
                            JSONObject obj = new JSONObject();
                            obj.put("state", s.isRunning() ? "started" : "stopped");
                            obj.put("server-id", server);
                            obj.put("readIndex", s.getCurrentWriteIndex());
                            obj.put("bufSize", s.getBufferSize());
                            JSONArray arr = new JSONArray();
                            for(Map.Entry<Setting, String> se : s.settings.entrySet()) {
                                JSONObject obj1 = new JSONObject();
                                obj1.put("name", se.getKey().getName());
                                obj1.put("readonly", se.getKey().isReadonly());
                                obj1.put("type", se.getKey().getType());
                                obj1.put("description", se.getKey().getName());
                                arr.put(obj1);
                            }
                            obj.put("settings", arr);
                            b.append(obj.toString());
                        }
                    }
                    break;
                    case "log": {
                        synchronized (s) {
                            JSONObject obj = new JSONObject();
                            obj.put("state", s.isRunning() ? "started" : "stopped");
                            obj.put("server-id", server);

                            long clientIndex = incoming.optLong("readIndex", 0);
                            long serverIndex = s.getCurrentWriteIndex();
                            if (clientIndex > serverIndex) {
                                clientIndex = 0;
                            }
//                            long bufferSize = s.getCurrentBufferSize();
//                            long newIndex;
//                            long readBytes;

                            byte[] clientBuff = this.clientbuff.get();

//                            if (clientIndex <= serverIndex - bufferSize) {
//                                clientIndex = serverIndex - bufferSize + 1;
//                                newIndex = s.readBytes(clientBuff, 0, clientBuff.length, clientIndex, false, 0);
//                                readBytes = clientBuff.length;
//                            } else {
//                                newIndex = s.readBytes(clientBuff, 0, clientBuff.length, clientIndex, true, 2);
//                                readBytes = Math.min(newIndex - clientIndex, clientBuff.length);
//                                serverIndex = s.getCurrentWriteIndex();
//                            }
                            Logger.getLogger(GameConsole.class.getName()).log(Level.INFO, "Request log files: {0} - {1} : {2}",
                                    new Object[]{s.getLowestValidReadIndex(), s.getCurrentWriteIndex(), clientIndex});
                            int r = s.readBytes(clientBuff, 0, clientBuff.length, clientIndex);
                            if (r == -1) {
                                clientIndex = s.getLowestValidReadIndex();
                                r = s.readBytes(clientBuff, 0, clientBuff.length, clientIndex);
                                assert r > 0;
                            } else if (r == 0 && incoming.optBoolean("blocking", false)) {
                                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
                                ctx.flush();
                                r = s.readBytesBlocking(clientBuff, 0, clientBuff.length, clientIndex, 8);
                            }

                            if (r < 0) {
                                obj.put("log", "");
                                r = 0;
                            } else {
                                obj.put("log", new String(clientBuff, 0, (int) r).replace("\\", "\\\\").replace("\n", "\\n"));
                            }
                            obj.put("oldReadIndex", clientIndex);
                            obj.put("nextReadIndex", clientIndex + r);
                            obj.put("readIndex", serverIndex);

                            b.append(obj.toString());
                        }
                    }
                    break;
                    case "forcestop": {
                        synchronized (s) {
                            if (s.isRunning()) {
                                s.forceStop();
                            }
                            JSONObject obj = new JSONObject();
                            obj.put("state", s.isRunning() ? "started" : "stopped");
                            obj.put("server-id", server);
                            obj.put("readIndex", s.getCurrentWriteIndex());
                            obj.put("bufSize", s.getBufferSize());
                            b.append(obj.toString());
                        }
                    }
                    break;
                    case "status": {
                        synchronized (s) {
                            JSONObject obj = new JSONObject();
                            obj.put("state", s.isRunning() ? "started" : "stopped");
                            obj.put("server-id", server);
                            obj.put("readIndex", s.getCurrentWriteIndex());
                            obj.put("bufSize", s.getBufferSize());
                            b.append(obj.toString());
                        }
                    }
                    break;
                    case "command": {
                        synchronized (s) {
                            if (s.isRunning()) {
                                s.sendCommand(incoming.optString("command", ""));
                            }
                            JSONObject obj = new JSONObject();
                            obj.put("state", s.isRunning() ? "started" : "stopped");
                            obj.put("server-id", server);
                            obj.put("readIndex", s.getCurrentWriteIndex());
                            obj.put("bufSize", s.getBufferSize());
                            b.append(obj.toString());
                        }
                    }
                    break;

                    default: {
                        status.set(HttpResponseStatus.METHOD_NOT_ALLOWED);
                    }
                    return;
                }
            }
            break;
            case "management": {
                Logger.getLogger(GameConsole.class.getName()).log(Level.INFO, "Request: {0}: {1}", new Object[]{target, action});
                switch (action) {
                    case "servers": {

                        JSONObject obj = new JSONObject();
                        JSONArray array = new JSONArray();
                        for (String serverName : servers.keySet()) {
                            array.put(serverName);
                        }
                        obj.put("servers", array);
                        b.append(obj.toString());

                    }
                    break;
                    case "status": {

                        JSONObject obj = new JSONObject();
                        for (Map.Entry<String, Server> serverName : servers.entrySet()) {
                            JSONObject obj1 = new JSONObject();
                            final Server server = serverName.getValue();
                            synchronized (server) {
                                obj1.put("state", server.isRunning() ? "started" : "stopped");
                                obj1.put("server-id", serverName.getKey());
                                obj1.put("readIndex", server.getCurrentWriteIndex());
                                obj1.put("bufSize", server.getBufferSize());
                                obj1.put("exitCode", 0);
                                obj1.put("smalldescription", serverName.getKey());
                            }
                            obj.put(serverName.getKey(), obj1);
                        }
                        b.append(obj.toString());

                    }
                    break;
                    default:
                        status.set(HttpResponseStatus.METHOD_NOT_ALLOWED);
                        return;

                }
            }
            break;
            default:
                Logger.getLogger(GameConsole.class.getName()).log(Level.INFO, "Request: {0}", new Object[]{target});
                status.set(HttpResponseStatus.METHOD_NOT_ALLOWED);
                return;
        }

        byte[] temp = b.toString().getBytes(Charset.forName("UTF-8"));
        serverheaders.put("Content-type", "application/json; charset=utf-8");
        //rverheaders.put("Content-lenght", String.valueOf(temp.length));
        output.write(temp);
        status.set(HttpResponseStatus.OK);
    }

    public void shutdown() {
        for (Server server : this.servers.values()) {
            synchronized (server) {
                if (server.isRunning()) {
                    try {
                        server.sendCommand("stop");
                        server.sendCommand("stop");
                    } catch (IOException ex) {
                        Logger.getLogger(GameConsole.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        int runningServers;
        int attempt = 0;
        boolean interrupted = false;
        do {
            runningServers = 0;
            for (Server server : this.servers.values()) {
                synchronized (server) {
                    if (server.isRunning()) {
                        runningServers++;
                    }
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        } while (attempt++ < 10 && runningServers > 0);
        for (Server server : this.servers.values()) {
            synchronized (server) {
                if (server.isRunning()) {
                    server.forceStop();
                }
            }
        }
        if(interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
