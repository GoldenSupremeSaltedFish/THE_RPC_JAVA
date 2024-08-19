## 流程

接口调用-》序列化-》协议组装（最起码知道哪里是什么）-》网络io（netty）-》解析协议-》反序列化（拼接为请求或者响应）

### 思考：缺少一个请求的唯一id（来确定请求的）


### 客户端进行方法的调用，将自己的方法调用转换为request,(封装请求)
### 以下为转换方法
```java

 @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //构建request,将客户端的请求方法转换为request
        RpcRequest request=RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args).paramsType(method.getParameterTypes()).build();
        //IOClient.sendRequest 和服务端进行数据传输
        RpcResponse response= IOClient.sendRequest(host,port,request);
        return response.getData();//进行获取运算后的数据

```

### 以下为RPCrequest的结构体

```java

@Data
@Builder
public class RpcRequest implements Serializable {
    //服务类名，客户端只知道接口
    private String interfaceName;
    //调用的方法名
    private String methodName;
    //参数列表
    private Object[] params;
    //参数类型
    private Class<?>[] paramsType;
}

```

### request传递到IOClient进行底层数据通讯（to host）-在part2中client中增加了netty

``` java

public class IOClient {
    //这里负责底层与服务端的通信，发送request，返回response
    public static RpcResponse sendRequest(String host, int port, RpcRequest request){
        try {
            Socket socket=new Socket(host, port);
            ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois=new ObjectInputStream(socket.getInputStream());

            oos.writeObject(request);//将流进行写入流中
            oos.flush();//强制刷新到基础流中

            RpcResponse response=(RpcResponse) ois.readObject();//获取回应体
            return response;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}

```

### 管理链接（单线程版本/线程池版本）


### part2

### 服务端代码

## 先进行服务的注册,将接口以及接口对应的实现类进行遍历，存储到hashmap中（多个服务端共享一个hashmap的时候需要考虑线程安全）

```java
private Map<String,Object> interfaceProvider;

    public ServiceProvider(){
        this.interfaceProvider=new HashMap<>();
    }

    public void provideServiceInterface(Object service){
        String serviceName=service.getClass().getName();
        //获取接口名称
        Class<?>[] interfaceName=service.getClass().getInterfaces();
        //一个接口可能有多个实现类，所以需要遍历

        for (Class<?> clazz:interfaceName){
            interfaceProvider.put(clazz.getName(),service);
        }

    }

    public Object getService(String interfaceName){
        return interfaceProvider.get(interfaceName);
    }

```
## 服务端进行request的处理，以及打包到response中

``` java
public void run() {
        try {
            ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois=new ObjectInputStream(socket.getInputStream());
            //读取客户端传过来的request
            RpcRequest rpcRequest = (RpcRequest) ois.readObject();
            //反射调用服务方法获取返回值
            RpcResponse rpcResponse=getResponse(rpcRequest);
            //向客户端写入response
            oos.writeObject(rpcResponse);
            oos.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private RpcResponse getResponse(RpcRequest rpcRequest){
        //得到服务名
        String interfaceName=rpcRequest.getInterfaceName();
        //得到服务端相应服务实现类
        Object service = serviceProvide.getService(interfaceName);
        //反射调用方法
        Method method=null;
        try {
            method= service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamsType());
            Object invoke=method.invoke(service,rpcRequest.getParams());
            return RpcResponse.sussess(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RpcResponse.fail();
        }
    }

```
### 引入zookeeper进行注册中心的应用

zookeeper部署在相同的位置

### 服务端zookeeper

```java
public ZKServiceRegister(){
        // 指数时间重试
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);// 重试间隔1s，重试次数3次
        // zookeeper的地址固定，不管是服务提供者还是，消费者都要与之建立连接
        // sessionTimeoutMs 与 zoo.cfg中的tickTime 有关系，
        // zk还会根据minSessionTimeout与maxSessionTimeout两个参数重新调整最后的超时值。默认分别为tickTime 的2倍和20倍
        // 使用心跳监听状态
        this.client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();// 启动zookeeper
        //启动的时候，zookeeper会创建一个根节点，这个根节点就是ROOT_PATH
        //40s的心跳检测时间
        // 使用心跳监听状态
        //retryPolicy(policy)：设置重试策略。policy是一个RetryPolicy对象，
        //用于定义在连接或操作失败时进行重试的方式。在你提供的代码中，使用的是ExponentialBackoffRetry重试策略，
        //它会在连接失败时进行指数级的重试，每次重试的间隔时间会逐渐增加。这里的policy是之前定义的ExponentialBackoffRetry对象。
        // 设置命名空间。命名空间是一种将ZooKeeper的节点分组的方式，可以用于隔离不同的应用或服务。在你提供的代码中
        // ，命名空间被设置为MyRPC，即根路径节点的名称。
        this.client.start();// 启动zookeeper
        System.out.println("zookeeper 连接成功");
    }

```


### 客户端zookeeper

（一样的做法，改一个类名就ok）

### 难点：怎样进行自定义的序列化器，编码器和解码器实现

### 自定义的序列化器

#### 消息格式(序列化器类型+序列化数组长度+序列化数组)
#### 消息类型由getcode确定
枚举定义request和response

方便编码器和解码器进行分析
```java
public enum MessageType {
    REQUEST(0),RESPONSE(1);
    private int code;
    public int getCode(){
        return code;
    }
}

```
#### 编码器的使用和实现
```java
private Serializer serializer;//需要持有一个序列化器
 @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        System.out.println(msg.getClass());
        //1.写入消息类型
        if(msg instanceof RpcRequest){
            out.writeShort(MessageType.REQUEST.getCode());
        }//在此进行请求和回应的区分，将分类写入缓冲区
        else if(msg instanceof RpcResponse){
            out.writeShort(MessageType.RESPONSE.getCode());
        }
        //2.写入序列化方式
        out.writeShort(serializer.getType());
        //得到序列化数组
        byte[] serializeBytes = serializer.serialize(msg);//to 各种的序列化器，在此实现了两种序列化器
        //3.写入长度
        out.writeInt(serializeBytes.length);
        //4.写入序列化数组
        out.writeBytes(serializeBytes);
    }

```
#### 解码器的实现
```java

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        //1.读取消息类型
        short messageType = in.readShort();
        // 现在还只支持request与response请求
        if(messageType != MessageType.REQUEST.getCode() &&
                messageType != MessageType.RESPONSE.getCode()){
            System.out.println("暂不支持此种数据");
            return;
        }
        //2.读取序列化的方式&类型
        short serializerType = in.readShort();
        Serializer serializer = Serializer.getSerializerByCode(serializerType);
        if(serializer == null)
            throw new RuntimeException("不存在对应的序列化器");
        //3.读取序列化数组长度
        int length = in.readInt();
        //4.读取序列化数组
        byte[] bytes=new byte[length];
        in.readBytes(bytes);
        Object deserialize= serializer.deserialize(bytes, messageType);
        //反序列化
        out.add(deserialize);

```

#### 以下是序列化器的实现

#### json序列化

```java

public byte[] serialize(Object obj) {
        byte[] bytes = JSONObject.toJSONBytes(obj);
        return bytes;
    }

```
#### 默认的序列化

```java
public byte[] serialize(Object obj) {
        byte[] bytes=null;
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        try {
            //是一个对象输出流，用于将 Java 对象序列化为字节流，并将其连接到bos上
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            //刷新 ObjectOutputStream，确保所有缓冲区中的数据都被写入到底层流中。
            oos.flush();
            //将bos其内部缓冲区中的数据转换为字节数组
            bytes = bos.toByteArray();
            oos.close();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;

```



#### 反序列化器的实现

#### 默认的反序列化

```java
public Object deserialize(byte[] bytes, int messageType) {
        Object obj = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }


```

#### json反序列化
```java

public Object deserialize(byte[] bytes, int messageType) {
        Object obj = null;
        // 传输的消息分为request与response
        switch (messageType){
            case 0:
                RpcRequest request = JSON.parseObject(bytes, RpcRequest.class);
                Object[] objects = new Object[request.getParams().length];
                // 把json字串转化成对应的对象， fastjson可以读出基本数据类型，不用转化
                // 对转换后的request中的params属性逐个进行类型判断
                for(int i = 0; i < objects.length; i++){
                    Class<?> paramsType = request.getParamsType()[i];
                    //判断每个对象类型是否和paramsTypes中的一致
                    if (!paramsType.isAssignableFrom(request.getParams()[i].getClass())){
                        //如果不一致，就行进行类型转换
                        objects[i] = JSONObject.toJavaObject((JSONObject) request.getParams()[i],request.getParamsType()[i]);
                    }else{
                        //如果一致就直接赋给objects[i]
                        objects[i] = request.getParams()[i];
                    }
                }
                request.setParams(objects);
                obj = request;
                break;
            case 1:
                RpcResponse response = JSON.parseObject(bytes, RpcResponse.class);
                Class<?> dataType = response.getDataType();
                //判断转化后的response对象中的data的类型是否正确
                if(! dataType.isAssignableFrom(response.getData().getClass())){
                    response.setData(JSONObject.toJavaObject((JSONObject) response.getData(),dataType));
                }
                obj = response;
                break;
            default:
                System.out.println("暂时不支持此种消息");
                throw new RuntimeException();
        }
        return obj;
    }

```
### 目标-》对服务的注册列表进行持久化（redis或mysql）

### 实现动态刷新客户端的本地缓存

### 本地缓存的作用

1，让注册中心的访问频率降低

2，zookeeper挂了也暂时能用（如果你缓存没有设置过期事件的话）

3，缓存频繁请求的数据（白名单就一直存着吧）

### 负载均衡（轮询，纯随机，一致性哈希）

一致性哈希实现：每个真实节点拥有5个虚拟的节点

好处：数据均匀分布，故障容忍(哈希环的节点下线还可以分给下一个节点)，新增节点的时候数据分布十分缓和，不会出现大量数据的迁移

一致性哈希代码
```java

private static final int VIRTUAL_NUM = 5;

    // 虚拟节点分配，key是hash值，value是虚拟节点服务器名称
    // 哈希环
    private SortedMap<Integer, String> shards = new TreeMap<Integer, String>();

    // 真实节点列表
    private List<String> realNodes = new LinkedList<String>();

    //模拟初始服务器
    private String[] servers =null;

    private  void init(List<String> serviceList) {
        for (String server :serviceList) {
            realNodes.add(server);
            System.out.println("真实节点[" + server + "] 被添加");
            for (int i = 0; i < VIRTUAL_NUM; i++) {
                String virtualNode = server + "&&VN" + i;
                int hash = getHash(virtualNode);
                shards.put(hash, virtualNode);
                System.out.println("虚拟节点[" + virtualNode + "] hash:" + hash + "，被添加");
            }
        }
    }
    /**
     * 获取被分配的节点名
     *
     * @param node
     * @return
     */
    public  String getServer(String node,List<String> serviceList) {
        init(serviceList);
        int hash = getHash(node);
        Integer key = null;
        SortedMap<Integer, String> subMap = shards.tailMap(hash);
        if (subMap.isEmpty()) {
            key = shards.lastKey();
        } else {
            key = subMap.firstKey();//回到第一个键
        }
        String virtualNode = shards.get(key);
        return virtualNode.substring(0, virtualNode.indexOf("&&"));
    }

    /**
     * 添加节点
     *
     * @param node
     */
    public  void addNode(String node) {
        if (!realNodes.contains(node)) {
            realNodes.add(node);
            System.out.println("真实节点[" + node + "] 上线添加");
            for (int i = 0; i < VIRTUAL_NUM; i++) {
                String virtualNode = node + "&&VN" + i;
                int hash = getHash(virtualNode);
                shards.put(hash, virtualNode);
                System.out.println("虚拟节点[" + virtualNode + "] hash:" + hash + "，被添加");
            }
        }
    }

    /**
     * 删除节点
     *
     * @param node
     */
    public  void delNode(String node) {
        if (realNodes.contains(node)) {
            realNodes.remove(node);
            System.out.println("真实节点[" + node + "] 下线移除");
            for (int i = 0; i < VIRTUAL_NUM; i++) {
                String virtualNode = node + "&&VN" + i;
                int hash = getHash(virtualNode);
                shards.remove(hash);
                System.out.println("虚拟节点[" + virtualNode + "] hash:" + hash + "，被移除");
            }
        }
    }

    /**
     * FNV1_32_HASH算法
     */
    private static int getHash(String str) {
        final int p = 16777619;
        //
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++)
            hash = (hash ^ str.charAt(i)) * p;
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        // 进一步混合哈希值
        // 如果算出来的值为负数则取其绝对值
        if (hash < 0)
            hash = Math.abs(hash);
        return hash;
    }

    @Override
    public String balance(List<String> addressList) {
        String random= UUID.randomUUID().toString();
        return getServer(random,addressList);
    }


```
#### 重试逻辑（确定逻辑是否可以进行重试）

重试逻辑的开始区域（每次进行请求的时候进行判断-》错误+白名单=重试）

### 问题：白名单的生成？未找到名单的初始化

### 限流算法（令牌桶）

令牌桶空才进行生成（根据时间戳进行）


主要逻辑
```java
 @Override
    public synchronized boolean getToken() {
        //如果当前桶还有剩余，就直接返回
        if(curCapcity>0){
            curCapcity--;
            return true;
        }
        //如果桶无剩余，
        long current=System.currentTimeMillis();
        //如果距离上一次的请求的时间大于RATE的时间
        if(current-timeStamp>=RATE){
            //计算这段时间间隔中生成的令牌，如果>2,桶容量加上（计算的令牌-1）
            //如果==1，就不做操作（因为这一次操作要消耗一个令牌）
            if((current-timeStamp)/RATE>=2){
                curCapcity+=(int)(current-timeStamp)/RATE-1;
            }
            //保持桶内令牌容量<=10
            if(curCapcity>CAPACITY) curCapcity=CAPACITY;
            //刷新时间戳为本次请求
            timeStamp=current;
            return true;
        }
        //获得不到，返回false
        return false;
    }

```

#### 断路器的实现

```java

public synchronized boolean allowRequest() {
        long currentTime = System.currentTimeMillis();
        System.out.println("熔断swtich之前!!!!!!!+failureNum=="+failureCount);
        switch (state) {
            case OPEN:
                if (currentTime - lastFailureTime > retryTimePeriod) 
                
                {
                    state = CircuitBreakerState.HALF_OPEN;
                    resetCounts();//重置断路器
                    return true;
                }
                System.out.println("熔断生效!!!!!!!");
                return false;
            case HALF_OPEN:
                requestCount.incrementAndGet();
                return true;
            case CLOSED:
            default:
                return true;
        }
    }

```


