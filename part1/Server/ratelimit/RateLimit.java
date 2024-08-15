package part1.Server.ratelimit;

/**
 * 限流器
 */
public interface RateLimit {
    //获取访问许可
    boolean getToken();
}
