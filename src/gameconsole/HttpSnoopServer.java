package gameconsole;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class HttpSnoopServer {

    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;
    private static ServerBootstrap b;
    private static GameConsole g;
    private static Channel ch;
    
    private final static int PORT = 8080;

    public static void main(String[] args) throws Exception {
        // Configure the server.
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        b = new ServerBootstrap();
        g = new GameConsole();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new HttpSnoopServerInitializer(new GameConsole()));

        ch = b.bind(PORT).sync().channel();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                ch.close().syncUninterruptibly();
                g.shutdown();
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }));

    }
}
