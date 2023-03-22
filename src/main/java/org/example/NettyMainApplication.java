package org.example;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;

import java.util.Objects;

@Log4j2
public class NettyMainApplication {

	//Thread accept connection
	static EpollEventLoopGroup bossGroupLinux = null;//Linux only
	static NioEventLoopGroup bossGroup = null;//Linux only

	//Thread handle connection
	static EpollEventLoopGroup workerGroupLinux = null;//Linux & Windows
	static NioEventLoopGroup workerGroup = null;//Linux & Windows

	public static void main(String[] args) throws InterruptedException {

		Channel wsChannel = null;

		try {
			ServerBootstrap wsBootstrap = initServerBootstrap();
			wsBootstrap.childHandler(new WebSocketInitializer());
			wsChannel = wsBootstrap.bind(8050).sync().channel();
		} catch (Exception e) {
			log.error("Start websocket: " + ExceptionUtils.getStackTrace(e));
		} finally {
			if (Objects.nonNull(wsChannel)) {
				wsChannel.closeFuture().sync();
			}

			if (Epoll.isAvailable()) {
				bossGroupLinux.shutdownGracefully();
				workerGroupLinux.shutdownGracefully();
			} else {
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		}
	}

	public static ServerBootstrap initServerBootstrap() {

		ServerBootstrap bootstrap = new ServerBootstrap();

		//Get current processor core of machine
		int coreProcess = Runtime.getRuntime().availableProcessors();

		//Set epoll if available, only available on linux
		if (Epoll.isAvailable()) {
			bossGroupLinux = new EpollEventLoopGroup(coreProcess);
			workerGroupLinux = new EpollEventLoopGroup(coreProcess * 2);
			//Set option applied to server socket (Server channel) that is listening for connections
			bootstrap.group(bossGroupLinux, workerGroupLinux)
					.option(ChannelOption.SO_BACKLOG, 32 * 1024)// Set maximum queue length
					.option(ChannelOption.SO_REUSEADDR, true)
					.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT) // use pooled buffer
					//Set childoption applied to socket that gets created once the connection is accepted by the server socket

					.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT) // use pooled buffer
					.childOption(ChannelOption.SO_REUSEADDR, true)
					.childOption(ChannelOption.SO_LINGER, 0)//remove connection immediately after disconnect
					//Prevent user send too quick message
					.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(384 * 1024, 512 * 1024))
					.childOption(ChannelOption.SO_SNDBUF, 128 * 1024)//maximum sending buffer size
					.childOption(ChannelOption.SO_RCVBUF, 128 * 1024)//maximum receiving buffer size
					.childOption(ChannelOption.TCP_NODELAY, true);

			//add epoll channel, only available with linux
			bootstrap.channel(EpollServerSocketChannel.class);
		} else {
			bossGroup = new NioEventLoopGroup(coreProcess);
			workerGroup = new NioEventLoopGroup(coreProcess * 2);
			//Set option applied to server socket (Server channel) that is listening for connections
			bootstrap.group(bossGroup, workerGroup)
					.option(ChannelOption.SO_BACKLOG, 32 * 1024)// Set maximum queue length
					.option(ChannelOption.SO_REUSEADDR, true)
					.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT) // use pooled buffer
					//Set childoption applied to socket that gets created once the connection is accepted by the server socket
					.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)// use pooled buffer
					.childOption(ChannelOption.SO_REUSEADDR, true)
					.childOption(ChannelOption.SO_LINGER, 0)//remove connection immediately after disconnect
					//Prevent user send too quick message
					.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(384 * 1024, 512 * 1024))
					.childOption(ChannelOption.SO_SNDBUF, 128 * 1024)//maximum sending buffer size
					.childOption(ChannelOption.SO_RCVBUF, 128 * 1024)//maximum receiving buffer size
					.childOption(ChannelOption.TCP_NODELAY, true);

			//add NIO channel
			bootstrap.channel(NioServerSocketChannel.class);
		}
		return bootstrap;
	}
}
