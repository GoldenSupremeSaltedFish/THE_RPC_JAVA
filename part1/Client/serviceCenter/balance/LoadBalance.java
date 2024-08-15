package part1.Client.serviceCenter.balance;

import java.util.List;

/**
 * 给服务地址列表，根据不同的负载均衡策略选择一个
 */
public interface LoadBalance {
    String balance(List<String> addressList);
    void addNode(String node) ;
    void delNode(String node);
}