package part1.Server.server.impl;


import lombok.AllArgsConstructor;
import part1.Server.server.RpcServer;
import part1.Server.server.work.WorkThread;
import part1.Server.provider.ServiceProvider;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 服务端实现类
 */
@AllArgsConstructor
public class SimpleRPCRPCServer implements RpcServer {
    private ServiceProvider serviceProvide;
    @Override
    public void start(int port) {
        try {
            ServerSocket serverSocket=new ServerSocket(port);
            System.out.println("服务器启动了");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new WorkThread(socket,serviceProvide)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {

    }
}
