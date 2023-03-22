package org.example;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.TimeUnit;
//</editor-fold>


@ChannelHandler.Sharable
@Log4j2
@AllArgsConstructor
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<Object> {
	private WebSocketServerHandshaker handshaker;

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
			ctx.channel().pipeline().addAfter("wsProtocol", "idleHandle", new IdleStateHandler(900, 0, 0, TimeUnit.SECONDS) {
				@Override
				protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
					ctx.close();
				}
			});
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object frame) {
		if (frame instanceof WebSocketFrame) {
			if (frame instanceof TextWebSocketFrame) {
				String message = ((TextWebSocketFrame) frame).text();
				if (message.startsWith("{")) {
					handleWebSocketFrame(ctx, message);
				} else {
					handleWebSocketFrameBenchMark(ctx, (TextWebSocketFrame) frame);
				}
			} else {
				handleWebSocketFrameBenchMark(ctx, (WebSocketFrame) frame);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (!cause.getMessage().contains("Connection reset by peer")) {
			log.error("Error when handle websocket " + ExceptionUtils.getStackTrace(cause));
		}
		ctx.close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		log.info("inactive");
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, String message) {
		ctx.channel().writeAndFlush(message);
	}

	private void handleWebSocketFrameBenchMark(ChannelHandlerContext ctx, WebSocketFrame frame) {

		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}
		if (frame instanceof PingWebSocketFrame) {
			ctx.write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		if (frame instanceof TextWebSocketFrame) {
			// Echo the frame
			ctx.write(frame.retain());
			return;
		}
		if (frame instanceof BinaryWebSocketFrame) {
			// Echo the frame
			ctx.write(frame.retain());
		}
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) {
		ctx.channel().config().setAutoRead(ctx.channel().isWritable());
	}

}
