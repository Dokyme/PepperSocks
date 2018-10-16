package com.PepperSocks;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import io.netty.handler.codec.socks.SocksRequest;

public class SocksServerHandler extends SimpleChannelInboundHandler<SocksRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksRequest msg) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        final Channel inBoundChannel = ctx.channel();
        SocksCmdRequest cmdRequest = (SocksCmdRequest) msg;
        bootstrap.group(inBoundChannel.eventLoop())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //在bootstrap完成TCP连接之后，会拿到远端的channel，马上在代理服务器与远端之间的pipeline中添加一个中继handler
                        //该中继负责将远端发送回来的数据转发给inBoundChannel，即被代理的客户端程序。
                        ch.pipeline().addLast(new SocksRelayHandler(inBoundChannel));
                    }
                });
        final ChannelFuture channelFuture = bootstrap.connect(cmdRequest.host(), cmdRequest.port());
        final Channel outBoundChannel = channelFuture.channel();
        ctx.pipeline().remove(this);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                //当然我认为在上面initChannel里面返回成功消息也是可以的
                System.out.println("Connected remote");
                if (channelFuture.isSuccess()) {
                    inBoundChannel.pipeline().addLast(new SocksRelayHandler(outBoundChannel));
                    inBoundChannel.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, cmdRequest.addressType()));
                } else {
                    inBoundChannel.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, cmdRequest.addressType()));
                    inBoundChannel.close();
                }
            }
        });
    }
}
