package org.example;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;


public class WebSocketInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        //Decodes bytes to HttpRequest, HttpContent,and LastHttpContent. Encodes HttpRequest,HttpContent, and LastHttpContent to bytes.
        pipeline.addLast("httpServerCodec", new HttpServerCodec());

        //Aggregates an HttpMessage and its following HttpContents into a single FullHttpRequest or FullHttpResponse With this installed, the next ChannelHandler in the pipeline will receive only full HTTP requests
        pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(65536));

        //handles the WebSocket upgrade handshake, PingWebSocketFrames, PongWebSocketFrames, and CloseWebSocketFrames.
        pipeline.addLast("httpProtocol", new HttpRequestHandler());

        //handles the WebSocket upgrade handshake, PingWebSocketFrames, PongWebSocketFrames, and CloseWebSocketFrames.
//        pipeline.addLast("wsProtocol", new WebSocketServerProtocolHandler("/ws", null, true, 8192, true));

        // handles the WebSocket message.
//        pipeline.addLast("WS frame handler", new WebSocketFrameHandler());
    }

}
