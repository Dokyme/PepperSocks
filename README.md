# PepperSocks

试水Netty之作

## 参考资料

[rfc1928](http://www.ietf.org/rfc/rfc1928.txt)

[socks-proxy-by-netty](https://github.com/code4craft/netty-learning/blob/master/posts/socks-proxy-by-netty.md)

## 相关问题

**为什么通过运行``curl -socks5 127.0.0.1:1080 www.baidu.com``命令，无法得到正确的结果？**

我认为并不是所有程序的Socks客户端都是严格按照rfc的标准来实现的，在用Wireshark抓包的过程中就遇到了以下3种情况：

1. 没有InitReqeust，直接就发送HTTP请求的，如curl、Python的requests库。

2. 直接发送CmdRequest（如Connect某个远程主机的），如Python的pip工具。

3. 完全按照握手规则来的，如MacOS的系统代理。