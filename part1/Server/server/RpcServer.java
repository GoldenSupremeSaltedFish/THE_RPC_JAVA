package part1.Server.server;

/**
 * 服务端接口
 */
public interface RpcServer {
    void start(int port);
    void stop();
}
