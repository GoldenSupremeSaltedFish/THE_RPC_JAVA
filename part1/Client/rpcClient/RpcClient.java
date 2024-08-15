package part1.Client.rpcClient;

import part1.common.Message.RpcResponse;
import part1.common.Message.RpcRequest;


public interface   RpcClient {

    //定义底层通信的方法
    RpcResponse sendRequest(RpcRequest request);
}
