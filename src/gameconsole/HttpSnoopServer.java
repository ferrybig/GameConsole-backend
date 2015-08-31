package gameconsole;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 */
public final class HttpSnoopServer {

    public static void main(String[] args) throws Exception {
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            GameConsole g = new GameConsole();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HttpSnoopServerInitializer(new GameConsole()));

            Channel ch = b.bind(8080).sync().channel();

            System.err.println("Open your web browser and navigate to http://127.0.0.1:8080/");

            ch.closeFuture().sync();
            g.shutdown();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}