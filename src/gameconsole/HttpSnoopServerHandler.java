package gameconsole;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import java.util.Map;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpSnoopServerHandler extends SimpleChannelInboundHandler<Object> {

    private final GameConsole console;

    private HttpRequest request;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] buf = new byte[0];
    private final AtomicReference<HttpResponseStatus> status = new AtomicReference<>(HttpResponseStatus.OK);
    private static final ThreadLocal<SimpleDateFormat> HTTP_TIME_FORMATTER = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat s = new SimpleDateFormat(
                    "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			s.setTimeZone(TimeZone.getTimeZone("GMT"));
			return s;
        }

    };

    HttpSnoopServerHandler(GameConsole console) {
        this.console = console;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;

            ByteBuf content = httpContent.content();
            String cont = "";
            if (content.isReadable()) {
                cont = content.toString(CharsetUtil.UTF_8);
            }
            headers.clear();
            if (request.getMethod() == HttpMethod.OPTIONS) {
                if (request.getUri().equals("/service")
                        || request.getUri().equals("/service?service=config")
                        || request.getUri().equals("/service?service=password")) {
                    status.set(NO_CONTENT);
                    headers.put("Allow", "GET,POST,OPTIONS,HEAD");
                    headers.put("cache-control", "max-age=120000");
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, 7);
                    headers.put("Expires",HTTP_TIME_FORMATTER.get().format(calendar.getTime())); // 1 week in future.
                    this.buf = new byte[0];
                } else {
                    status.set(NOT_FOUND);
                }

            } else {
                if (request.getUri().equals("/service")
                        || request.getUri().equals("/service?service=config")
                        || request.getUri().equals("/service?service=password")) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        this.console.handleRequest(out, request.headers(), headers, status, request.getUri(), cont, ctx);
                    } catch (IOException ex) {
                        Logger.getLogger(HttpSnoopServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(HttpSnoopServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                        Thread.currentThread().interrupt();
                    }
                    this.buf = out.toByteArray();
                } else {
                    status.set(NOT_FOUND);
                }
            }

            if (msg instanceof LastHttpContent) {
                LastHttpContent trailer = (LastHttpContent) msg;

                if (!writeResponse(trailer, ctx)) {
                    // If keep-alive is off, close the connection once the content is fully written.
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
    }

    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        // Build the response object.
        HttpResponseStatus s = status.get();

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, currentObj.getDecoderResult().isSuccess() ? s : BAD_REQUEST,
                Unpooled.wrappedBuffer(buf, 0, buf.length));
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            response.headers().set(entry.getKey(), entry.getValue());
        }
        response.headers().set("Access-Control-Allow-Headers", "Content-Type");
        response.headers().set("Access-Control-Allow-Methods", "POST");
        response.headers().set("Access-Control-Allow-Origin", "*");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        if (keepAlive) {
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the response.
        ctx.write(response);

        return keepAlive;
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
