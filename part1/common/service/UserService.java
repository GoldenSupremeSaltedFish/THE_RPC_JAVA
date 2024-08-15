package part1.common.service;


import part1.common.pojo.User;

/* 
 * 定义一个接口，服务端和客户端都需要实现这个接口
 */
public interface UserService {
    // 客户端通过这个接口调用服务端的实现类
    User getUserByUserId(Integer id);
    //新增一个功能
    Integer insertUserId(User user);
}
