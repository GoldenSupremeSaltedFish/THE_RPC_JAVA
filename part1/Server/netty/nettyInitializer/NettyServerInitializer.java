package part1.Server.netty.nettyInitializer;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.AllArgsConstructor;
import part1.Server.netty.handler.NettyRPCServerHandler;
import part1.Server.provider.ServiceProvider;
import part1.common.serializer.myCode.MyDecoder;
import part1.common.serializer.myCode.MyEncoder;
import part1.common.serializer.mySerializer.JsonSerializer;

/**
 * 初始化连接时候的各个组件
 */
@AllArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private ServiceProvider serviceProvider;
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //使用自定义的编/解码器
        pipeline.addLast(new MyEncoder(new JsonSerializer()));
        pipeline.addLast(new MyDecoder());
        pipeline.addLast(new NettyRPCServerHandler(serviceProvider));
    }
}
