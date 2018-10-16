package com.PepperSocks;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.*;

public class SocksShakeHandHandler extends SimpleChannelInboundHandler<SocksRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksRequest msg) throws Exception {
        System.out.println("SocksShakeHandHandler channelRead0");
        /**
         * Socks5连接与请求步骤（客户端、代理程序、远端）：
         * 1.客户端与代理程序完成三次握手（Socks5也支持UDP），发送一个版本协商和认证方式请求（InitRequest）。
         *                    +----+----------+----------+
         *                    |VER | NMETHODS | METHODS  |
         *                    +----+----------+----------+
         *                    | 1  |    1     | 1 to 255 |
         *                    +----+----------+----------+
         * 2.代理程序回复一个InitResponse，包含一个版本号以及最终选择的认证方式。
         *                          +----+--------+
         *                          |VER | METHOD |
         *                          +----+--------+
         *                          | 1  |   1    |
         *                          +----+--------+
         * 跳过认证相关步骤，RFC中没有写。。。
         * 3.客户端发送请求，请求可能有不同的目的。
         *         +----+-----+-------+------+----------+----------+
         *         |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
         *         +----+-----+-------+------+----------+----------+
         *         | 1  |  1  | X'00' |  1   | Variable |    2     |
         *         +----+-----+-------+------+----------+----------+
         * 4.代理程序给客户端一个响应。
         *         +----+-----+-------+------+----------+----------+
         *         |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
         *         +----+-----+-------+------+----------+----------+
         *         | 1  |  1  | X'00' |  1   | Variable |    2     |
         *         +----+-----+-------+------+----------+----------+
         * 5.然后代理程序开始充当客户端与远端之间的中介进行通讯。
         **/
        switch (msg.requestType()) {
            case INIT:
                System.out.println("Init");
                //Init阶段结束之后，客户端会发送CmdRequest，因此要准备解码。
                ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                //SocksInitResponsed的参数是SocksAuthScheme,可以选择几种认证方式中的一种，这里强制要求客户端不要认证。
                ctx.pipeline().writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                break;
            case AUTH:
                System.out.println("Auth");
                //实际上并不会进入到这里。。。。。。
                ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                ctx.pipeline().writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                break;
            case CMD:
                System.out.println("Cmd");
                SocksCmdRequest cmdRequest = (SocksCmdRequest) msg;
                if (cmdRequest.cmdType().equals(SocksCmdType.CONNECT)) {
                    ctx.pipeline().addLast(new SocksServerHandler());
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(cmdRequest);
                } else {
                    ctx.close();
                }
                break;
            case UNKNOWN:
                ctx.close();
                break;
            default:
                break;
        }
    }
}
