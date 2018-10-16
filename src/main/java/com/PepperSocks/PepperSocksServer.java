package com.PepperSocks;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;

public class PepperSocksServer {

    private int port;

    public PepperSocksServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        new PepperSocksServer(1080).run();
    }

    public void run() {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            System.out.println("initChannel");
                            ch.pipeline()
                                    //添加一个初始化请求解码器，把字节流转成SocksInitRequest
                                    .addLast(new SocksInitRequestDecoder())
                                    //出站方向添加一个Socks消息编码器，实际上就是把msg变成byte数组的过程
                                    .addLast(new SocksMessageEncoder())
                                    //自己的状态机，负责初始化-认证-命令-传输状态的控制
                                    .addLast(new SocksShakeHandHandler());
                        }
                    });
            ChannelFuture future = bootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

}
