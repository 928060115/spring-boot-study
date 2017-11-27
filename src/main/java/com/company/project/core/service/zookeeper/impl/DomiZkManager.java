package com.company.project.core.service.zookeeper.impl;

import com.company.project.core.service.zookeeper.MachineNode;
import com.company.project.core.service.zookeeper.ZkManager;
import com.company.project.core.service.zookeeper.watcher.DomiChildrenNodeHandler;
import com.company.project.core.service.zookeeper.watcher.DomiNodeHandler;
import com.company.project.core.service.zookeeper.watcher.DomiZkEventWatcher;
import com.company.project.core.service.dynProps4Files.DynProps4FilesService;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ZK 连接管理器<p>
 * <p>
 * 节点结构信息
 * <pre>
 * /domi_tc
 * |
 * +-machine_list
 *     |
 *     |-machine_name1: machine_info1
 *     +-machine_name2: machine_info2
 * </pre>
 *
 * @author jimersylee
 * 2017-09-25 14:34:54
 */
@Service
public class DomiZkManager implements ZkManager {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final Logger logger = LoggerFactory.getLogger(DomiZkManager.class);
    private static final int DEFAULT_SERVICE_PORT = 8080;
    private ZooKeeper zk = null;
    private static boolean inited = false;
    private static Lock LOCK = new ReentrantLock();

    private AtomicBoolean online = new AtomicBoolean(true);


    @Resource
    private DynProps4FilesService dynProps4FilesService;

    @Resource
    private Environment env;

    /**
     * 节点数据变化监听处理器
     */
    private static final ConcurrentHashMap<String/*节点路径*/, DomiNodeHandler> nodeChangedHandlerHolder = new ConcurrentHashMap<String, DomiNodeHandler>();
    /**
     * 子节点变化监听处理器
     */
    private static final ConcurrentHashMap<String/*节点路径*/, DomiChildrenNodeHandler> childrenChangedHandlerHolder = new ConcurrentHashMap<String, DomiChildrenNodeHandler>();

    /**
     * 需处理的ZK事件
     */
    private static volatile LinkedBlockingQueue<WatchedEvent> zkEventQueue = new LinkedBlockingQueue<WatchedEvent>(128);


    /**
     * ZK事件消费者
     */
    private List<EventConsumer> consumers = new ArrayList<EventConsumer>();

    /**
     * 事件消费者数量。同时该参数决定本机器最多可同时运行多少任务
     */
    private int eventConsumerNum = 2;

    /**
     * zk 连接字符串 ip:port,ip1:port1,...,ipn:portn
     */
    private String hostPort;

    /**
     * zk 会话超时时间(MS)
     */
    private int sessionTimeout = 3600000;

    /**
     * zk 全局事件处理器
     */
    private Watcher watcher = null;

    private CountDownLatch latch;

    /**
     * 自身节点监听器
     */
    private DomiNodeHandler localMachineHandler;

    /**
     * 自身节点对象
     */
    private MachineNode localMachine;

    /**
     * 本机节点zk路径
     */
    private String nodePath;


    public DomiZkManager() {

    }


    /**
     * 连接ZK 发布本机节点
     */
    @PostConstruct
    public void init() throws IOException {

        //构造本机节点
        if (localMachine == null) {

            localMachine = new MachineNode();
            String ip = "";
            ip = env.getProperty("LOCAL_HOST_NAME");
            if (StringUtils.isBlank(ip)) {
                ip = getLocalIpAddress();
            }
            localMachine.setHostname(ip);
            Integer port = null;
            port = env.getProperty("LOCAL_PORT", Integer.class);

            if (port == null) {
                port = DEFAULT_SERVICE_PORT;
            }
            localMachine.setPort(port);
        }

        if (watcher == null) {
            watcher = new DomiZkEventWatcher(this, localMachine);
        }


        boolean lockResult = LOCK.tryLock();
        //防止多个线程同时调本方法，若发现已有线程正在调此方法，则直接返回
        if (!lockResult) {
            return;
        }


        this.hostPort = env.getProperty("ZK_HOSTS");
        this.eventConsumerNum=env.getProperty("EVENT_CONSUMER_NUM",Integer.class);
        this.sessionTimeout=env.getProperty("SESSION_TIMEOUT",Integer.class);


        try {
            latch = new CountDownLatch(1);
            if (zk != null) {
                try {
                    zk.close();
                } catch (InterruptedException e) {
                    //do nothing
                }
            }


            if (!inited) {
                nodePath = TC_MACHINE_LIST_PATH + PATH_SPLITTER + localMachine.nodeName();
                Thread thread = null;
                EventConsumer ec = null;
                for (int i = 0; i < eventConsumerNum; i++) {
                    ec = new EventConsumer();
                    thread = new Thread(ec, "ZK-Event-Consumer-" + i);
                    thread.start();
                    //之所以把runnable加入LIST ，是为了在运行结束时，为EventConsumer 设置结束标志，让它正常结束线程
                    consumers.add(ec);
                }

                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (zk != null) {
                            try {
                                zk.close();
                            } catch (InterruptedException e) {
                                //do nothing
                            }
                        }
                        if (!CollectionUtils.isEmpty(consumers)) {
                            //停止zk事件消费线程
                            for (EventConsumer consumer : consumers) {
                                consumer.shutdown = true;
                            }
                        }

                    }
                }));

                //注册默认监听器
                if (localMachineHandler != null) {
                    nodeChangedHandlerHolder
                            .putIfAbsent(TC_MACHINE_LIST_PATH + PATH_SPLITTER + localMachine.nodeName(), localMachineHandler);
                }

                inited = true;
            }

            zk = new ZooKeeper(hostPort, sessionTimeout, watcher);

            if (StringUtils.equalsIgnoreCase(dynProps4FilesService.getProperty("CLOSE_ZK_CNN", "FALSE"), "TRUE")) {
                //仅在本地环境使用
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    //do nothing
                }
                try {
                    this.offlineLocalMachineNode();
                } catch (Exception e) {
                    logger.error("Offline failed.", e);
                }
            }


        } finally {
            LOCK.unlock();
        }
    }


    /**
     * 等待建立连接
     */
    @Override
    public void waitForConnected() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
    }

    @Override
    public void countDownWaitForConnectedLatch() {
        latch.countDown();
    }

    /**
     * 追加监视的ZK事件
     *
     * @param event
     */
    @Override
    public void addEvent(WatchedEvent event) {
        if (event == null) {
            logger.info("event is null");
            return;
        }
        //在这里过滤不需要处理的事件
        String path = event.getPath();
        if (!nodeChangedHandlerHolder.containsKey(path) && !childrenChangedHandlerHolder.containsKey(path)) {
            logger.info("No handler found.for path[{}]", path);
            return;
        }


        try {
            zkEventQueue.put(event);
            logger.info("event is add into zkEventQueue.EVENT[{}]", event);
        } catch (InterruptedException e) {
            logger.error("Adding zk event failed! [" + event + "]", e);
        }
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Watcher getWatcher() {
        return watcher;
    }

    public void setWatcher(Watcher watcher) {
        this.watcher = watcher;
    }

    /**
     * 发布非持久化节点
     *
     * @param path
     * @param value
     * @return 实际创建的节点路径
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    public String createEphemeralNode(String path, String value) throws KeeperException, InterruptedException {

        try {
            return zk.create(path, value.getBytes(DEFAULT_CHARSET), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (UnsupportedEncodingException e) {
            //unreachable
            return null;
        }
    }


    /**
     * 发布持久化节点
     *
     * @param path
     * @param value
     * @return 实际创建的节点路径
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    public String createNode(String path, String value) throws KeeperException, InterruptedException {
        waitForConnected();

        try {
            return zk.create(path, value.getBytes(DEFAULT_CHARSET), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (UnsupportedEncodingException e) {
            //unreachable
            return null;
        }
    }

    /**
     * 取当前节点路径的父路径。
     *
     * @param nodePath
     * @return 父路径。当前节点为"/"时，则返回null.
     */
    private static String getParentPath(String nodePath) {
        if (StringUtils.isBlank(nodePath)) {
            throw new IllegalArgumentException("Param nodePath should not be blank!");
        }
        if (StringUtils.equals(PATH_SPLITTER, StringUtils.trim(nodePath))) {
            return null;
        }

        return nodePath.substring(0, nodePath.lastIndexOf(PATH_SPLITTER));
    }


    /**
     * 创建节点。若父节点不存在则先创建父节点
     *
     * @param path
     * @param value
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String createNodeRecursively(String path, String value) throws KeeperException, InterruptedException {
        waitForConnected();
        String parentPath = getParentPath(path);

        if (!exists(parentPath)) {
            createNodeRecursively(parentPath, "");
        }

        return createNode(path, value);
    }

    /**
     * @param path
     * @param value
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    public Stat updateNodeData(String path, String value) throws KeeperException, InterruptedException {
        return updateNodeData(path, value, -1);
    }

    /**
     * 创建节点信息，若该节点已存在则更新此节点数据
     *
     * @param path
     * @param value
     */
    @Override
    public void createOrUpdateNodeData(String path, String value) throws KeeperException, InterruptedException {
        if (exists(path)) {
            updateNodeData(path, value);
        } else {
            createNodeRecursively(path, value);
        }
    }

    /**
     * 判断节点是否存在
     *
     * @param path
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    public boolean exists(String path) throws KeeperException, InterruptedException {
        return exists(path, false);
    }

    /**
     * 判断节点是否存在
     *
     * @param path
     * @param watch 监听此节点create 、delete事件
     * @return
     * @throws org.apache.zookeeper.KeeperException
     * @throws InterruptedException
     */
    @Override
    public boolean exists(String path, boolean watch) throws KeeperException, InterruptedException {
        waitForConnected();
        Stat stat = zk.exists(path, watch);
        return (stat != null);
    }

    /**
     * 删除节点
     *
     * @param path
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    public void deleteNode(String path) throws KeeperException, InterruptedException {
        waitForConnected();
        zk.delete(path, -1);
    }


    /**
     * 递归删除节点
     *
     * @param path
     */
    @Override
    public void deleteNodeRecursively(String path) throws KeeperException, InterruptedException {
        waitForConnected();
        if (StringUtils.equals(PATH_SPLITTER, StringUtils.trim(path))) {
            return;
        }
        List<String> children = getChildren(path);
        if (children != null && !children.isEmpty()) {
            for (String child : children) {
                deleteNodeRecursively(path + PATH_SPLITTER + child);
            }
        }

        deleteNode(path);
    }

    /**
     * 为特定路径的节点 添加监听器（同时监听数据变化以及该节点存在性变化）
     *
     * @param path
     * @param handler
     */
    @Override
    public void addNodeDataChangedHandler(String path,
                                          DomiNodeHandler handler) throws KeeperException, InterruptedException {
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Handling path for node is BLANK!");
        }
        Object previous = nodeChangedHandlerHolder.put(path, handler);
        if (previous == null) {
            //注册监听
            getData(path, true);
            exists(path, true);
        }
    }

    /**
     * 为特定路径的节点 添加监听器（监听数子节点数量变化）
     *
     * @param path
     * @param handler
     */
    @Override
    public void addChildrenChangedHandler(String path,
                                          DomiChildrenNodeHandler handler) throws KeeperException, InterruptedException {
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Handling path for children is BLANK!");
        }
        Object previous = childrenChangedHandlerHolder.put(path, handler);
        if (previous == null) {
            //注册监听
            getChildren(path, true);
        }
    }

    /**
     * 追加子节点监听器（初始化前调用有效）
     *
     * @param childrenNodeHandlerMap
     */
    @Override
    public void setChildrenChangedHandlerBeforeInit(
            Map<String/*节点路径名称*/, DomiChildrenNodeHandler> childrenNodeHandlerMap) {

        childrenChangedHandlerHolder.putAll(childrenNodeHandlerMap);
    }


    /**
     * 追加节点监听器（初始化前调用有效）
     *
     * @param nodeHandlerMap
     */
    @Override
    public void setNodeDataChangedHandlerBeforeInit(Map<String/*节点路径名称*/, DomiNodeHandler> nodeHandlerMap) {
        nodeChangedHandlerHolder.putAll(nodeHandlerMap);
    }

    /**
     * 获取节点数据
     *
     * @param path
     * @return
     */
    @Override
    public String getData(String path) throws KeeperException, InterruptedException {
        waitForConnected();
        return getData(path, false);
    }

    /**
     * 更新node数据
     *
     * @param path
     * @param value
     * @param version
     * @return
     * @throws org.apache.zookeeper.KeeperException
     * @throws InterruptedException
     */
    @Override
    public Stat updateNodeData(String path, String value, int version) throws KeeperException, InterruptedException {
        waitForConnected();
        try {
            return zk.setData(path, value.getBytes(DEFAULT_CHARSET), version);
        } catch (UnsupportedEncodingException e) {
            //unreachable
            return null;
        }
    }

    /**
     * 获取节点数据
     *
     * @param path
     * @param stat
     * @return
     * @throws InterruptedException
     * @throws org.apache.zookeeper.KeeperException
     */
    @Override
    public String getData(String path, Stat stat) throws KeeperException, InterruptedException {
        return getData(path, false, stat);
    }

    /**
     * 获取节点数据
     *
     * @param path
     * @param watch true:监听此节点的数据变化
     * @return
     */
    @Override
    public String getData(String path, boolean watch) throws KeeperException, InterruptedException {
        return getData(path, watch, null);
    }

    /**
     * 获取节点数据
     *
     * @param path
     * @param watch true:监听此节点的数据变化
     * @param stat
     * @return
     * @throws org.apache.zookeeper.KeeperException
     * @throws InterruptedException
     */
    @Override
    public String getData(String path, boolean watch, Stat stat) throws KeeperException, InterruptedException {
        waitForConnected();
        byte[] data = new byte[0];
        try {
            data = zk.getData(path, watch, stat);
        } catch (KeeperException e) {
            if (e instanceof KeeperException.NoNodeException) {
                return null;
            }
            throw e;
        }
        try {
            return new String(data, DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            //unreachable
            return null;
        }
    }

    /**
     * 获取path节点的子节点名称列表
     *
     * @param path
     * @return
     */
    @Override
    public List<String> getChildren(String path) throws KeeperException, InterruptedException {
        return getChildren(path, false);
    }

    /**
     * 获取path节点的子节点名称列表
     *
     * @param path
     * @param watch 监听节点个数变化的事件
     * @return
     */
    @Override
    public List<String> getChildren(String path, boolean watch) throws KeeperException, InterruptedException {
        waitForConnected();
        try {
            return zk.getChildren(path, watch);
        } catch (KeeperException e) {
            if (e instanceof KeeperException.NoNodeException) {
                logger.warn("", e);
                return null;
            }
            throw e;
        }
    }

    /**
     * 移除特定路径节点的监听器
     *
     * @param path
     */
    @Override
    public void removeNodeDataChangedHandler(String path) {
        if (path == null) {
            return;
        }

        nodeChangedHandlerHolder.remove(path);
    }

    /**
     * 移除特定路径节点的子节点处理器
     *
     * @param path
     */
    @Override
    public void removeChildrenChangedHandler(String path) {
        if (path == null) {
            return;
        }
        childrenChangedHandlerHolder.remove(path);
    }

    /**
     * 移除特定路径节点的所有处理器
     *
     * @param path
     */
    @Override
    public void removeHandlerByPath(String path) {
        removeChildrenChangedHandler(path);
        removeNodeDataChangedHandler(path);
    }

    /**
     * 获取本机节点对象
     *
     * @return
     */
    @Override
    public MachineNode getLocalMachine() {
        return this.localMachine;
    }


    /**
     * 从ZK端获取本机节点对象
     *
     * @return
     */
    @Override
    public MachineNode getLocalMachineFromZk() {

        MachineNode zkNode = null;
        try {
            Stat stat = new Stat();
            String value = getData(ZkManager.TC_MACHINE_LIST_PATH + PATH_SPLITTER + localMachine.nodeName(), stat);
            zkNode = simpleObjectMapper.readValue(value, MachineNode.class);

        } catch (Exception e) {
            logger.error("Get local machine value from zk is failed! ", e);
        }
        return zkNode;
    }

    /**
     * 判断是否有 path 节点的子节点监听处理器
     *
     * @param path
     */
    @Override
    public boolean containChildrenChangedHandler(String path) {
        return childrenChangedHandlerHolder.containsKey(path);
    }

    /**
     * 判断是否有 path 节点的监听处理器
     *
     * @param path
     * @return
     */
    @Override
    public boolean containNodeDataChangedHandler(String path) {
        return nodeChangedHandlerHolder.containsKey(path);
    }

    /**
     * 重新注册所有节点监听器
     */
    @Override
    public void refreshAllNodeDataChangedHandlers() {
        waitForConnected();
        for (String path : nodeChangedHandlerHolder.keySet()) {
            try {
                exists(path, true);
                getData(path, true);
            } catch (Exception e) {
                logger.error("Watching node changed failed! path[{}]", path, e);
            }
        }
        logger.warn("Refreshing all data-changed handlers completed! {}", nodeChangedHandlerHolder);
    }

    /**
     * 重新注册所有子节点监听器
     */
    @Override
    public void refreshAllChildrenChangedHandlers() {
        waitForConnected();
        for (String path : childrenChangedHandlerHolder.keySet()) {
            try {
                getChildren(path, true);
            } catch (Exception e) {
                logger.error("Watching node children changed failed! path[{}]", path, e);
            }
        }
        logger.warn("Refreshing all children-changed handlers completed! {}", childrenChangedHandlerHolder);
    }

    public DomiNodeHandler getLocalMachineHandler() {
        return localMachineHandler;
    }

    public void setLocalMachineHandler(DomiNodeHandler localMachineHandler) {
        this.localMachineHandler = localMachineHandler;
    }

    public int getEventConsumerNum() {
        return eventConsumerNum;
    }

    public void setEventConsumerNum(int eventConsumerNum) {
        this.eventConsumerNum = eventConsumerNum;
    }


    /**
     * 下线本机节点
     */
    @Override
    public void offlineLocalMachineNode() throws KeeperException, InterruptedException {
        this.online.set(false);
        //仅根据本机节点是否在线上进行判断
        if (this.exists(nodePath)) {
            this.deleteNode(nodePath);
        }
    }

    /**
     * 上线本机节点
     */
    @Override
    public void onlineLocalMachineNode() throws KeeperException, InterruptedException, IOException {
        if (!this.exists(nodePath)) {
            this.createEphemeralNode(nodePath, simpleObjectMapper.writeValueAsString(localMachine));
        }
        this.online.set(true);
    }

    /**
     * 判断本机节点是否已在zk上注册
     *
     * @return
     */
    @Override
    public boolean isLocalMachineOnline() {
        return this.online.get();
    }

    /**
     * 事件处理器
     */
    class EventConsumer implements Runnable {
        private volatile boolean shutdown = false;

        @Override
        public void run() {
            String dataPath = null;//节点路径
            while (!shutdown) {
                WatchedEvent event = null;
                try {
                    event = zkEventQueue.poll(2L, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    //do nothing
                }
                if (event == null) {
                    continue;
                }
                String value = null;
                DomiNodeHandler handler = null;
                List<String> childrenNameList = null;
                dataPath = event.getPath();
                if (StringUtils.isBlank(dataPath)) {
                    continue;
                }
                try {
                    switch (event.getType()) {
                        case NodeDataChanged:

                            handler = nodeChangedHandlerHolder.get(dataPath);
                            if (handler != null) {
                                value = DomiZkManager.this.getData(dataPath, true);
                                handler.handleDataChanged(dataPath, value, DomiZkManager.this);
                            }
                            break;
                        case NodeCreated:
                            handler = nodeChangedHandlerHolder.get(dataPath);
                            if (handler != null) {
                                DomiZkManager.this.exists(dataPath, true);
                                value = DomiZkManager.this.getData(dataPath, false);
                                handler.handleNodeCreated(dataPath, value, DomiZkManager.this);
                            }
                            break;
                        case NodeDeleted:
                            handler = nodeChangedHandlerHolder.get(dataPath);
                            if (handler != null) {
                                DomiZkManager.this.exists(dataPath, true);
                                handler.handleNodeDeleted(dataPath, DomiZkManager.this);
                            }
                            break;
                        case NodeChildrenChanged:
                            DomiChildrenNodeHandler childrenNodeHandler = childrenChangedHandlerHolder.get(
                                    dataPath);
                            if (childrenNodeHandler != null) {
                                childrenNameList = DomiZkManager.this.getChildren(dataPath, true);
                                childrenNodeHandler
                                        .handleChildrenChanged(dataPath, childrenNameList, DomiZkManager.this);
                            }
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    logger.error("执行处理器失败。ZK事件：", event.toString(), e);
                }

            }
        }
    }

    /**
     * 获取 本机非127.0.0.1 ipv4地址
     *
     * @return
     */
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface element = null;
            InetAddress addr = null;
            while (allNetInterfaces.hasMoreElements()) {
                element = allNetInterfaces.nextElement();
                Enumeration<InetAddress> addrs = element.getInetAddresses();

                while (addrs.hasMoreElements()) {
                    addr = addrs.nextElement();
                    if (addr == null) {
                        continue;
                    }
                    if (addr instanceof Inet4Address && !(StringUtils.equals("127.0.0.1", addr.getHostAddress()) || StringUtils.equals("0.0.0.0", addr.getHostAddress()))) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Getting localhost ip failed.", e);
            return "";
        }
        return "";
    }
}
