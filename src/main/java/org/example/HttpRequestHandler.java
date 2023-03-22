package org.example;

//<editor-fold defaultstate="collapsed" desc="IMPORT">

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.stomp.StompHeaders.CONTENT_LENGTH;
import static io.netty.handler.codec.stomp.StompHeaders.CONTENT_TYPE;
//</editor-fold>

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private WebSocketServerHandshaker handshaker;

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
		String clientIP = request.headers().get("X-Forwarded-For");
		if (clientIP == null) {
			InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
			clientIP = insocket.getAddress().getHostAddress();
		}
		AttributeKey<String> MY_KEY = AttributeKey.valueOf("IP");
		ctx.channel().attr(MY_KEY).set(clientIP);
		HttpHeaders headers = request.headers();

		if ("Upgrade".equalsIgnoreCase(headers.get(CONNECTION)) &&
				"WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {

			//Do the Handshake to upgrade connection from HTTP to WebSocket protocol
			handleHttpRequest(ctx, request);
		} else {
			ByteBuf content = Unpooled.copiedBuffer("OK", CharsetUtil.US_ASCII);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

			res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
			HttpUtil.setContentLength(res, content.readableBytes());
			sendHttpResponse(ctx, request, res);
			content.release();
		}

	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
		// Handle a bad request.
		if (!req.decoderResult().isSuccess()) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}

		// Handshake
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(ctx.channel().pipeline(), req), null, true, 5 * 1024 * 1024);
		handshaker = wsFactory.newHandshaker(req);
		if (Objects.isNull(handshaker)) {
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
			//Adding new handler to the existing pipeline to handle WebSocket Messages
			ctx.pipeline().replace(this, "websocketHandler", new WebSocketFrameHandler(handshaker));
		}
//		ctx.fireChannelRead(req.retain());
	}

	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!HttpUtil.isKeepAlive(req)) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static String getWebSocketLocation(ChannelPipeline cp, FullHttpRequest req) {
		String location = req.headers().get(HttpHeaderNames.HOST) + "/ws";
		if (Objects.nonNull(cp.get(SslHandler.class))) {
			return "wss://" + location;
		} else {
			return "ws://" + location;
		}
	}
}
