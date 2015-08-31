package gameconsole;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpSnoopServerInitializer extends ChannelInitializer<SocketChannel> {

    private final GameConsole console;

    public HttpSnoopServerInitializer(GameConsole console) {
        this.console = console;
    }
    
    
    @Override
    public void initChannel(SocketChannel ch) {
        System.out.println("Incoming connection:" + ch.remoteAddress());
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpRequestDecoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new HttpResponseEncoder());
        // Remove the following line if you don't want automatic content compression.
        //p.addLast(new HttpContentCompressor());
        p.addLast(new HttpSnoopServerHandler(console));
    }
}
