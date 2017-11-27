package com.company.project.core.service.zookeeper;

import com.company.project.core.service.zookeeper.watcher.DomiChildrenNodeHandler;
import com.company.project.core.service.zookeeper.watcher.DomiNodeHandler;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * ZK 连接管理接口
 *
 * @author qingren.lw
 *         16-10-27 下午5:19
 */
public interface ZkManager {
    ObjectMapper simpleObjectMapper = new ObjectMapper();
    /**
     * 多米交易中心 根节点根路径
     */
    String TC_ROOT_PATH = "/domi_tc";

    /**
     * 多米交易中心 机器节点根路径
     */
    String TC_MACHINE_LIST_PATH = TC_ROOT_PATH + "/machine_list";

    /**
     * zk路径分隔符 /
     */
    String PATH_SPLITTER="/";

    /**
     * 连接ZK 发布本机节点
     */
    void init() throws IOException;


    /**
     * 等待建立连接
     */
    void waitForConnected();

    void countDownWaitForConnectedLatch();

    /**
     * 追加监视的ZK事件
     *
     * @param event
     */
    void addEvent(WatchedEvent event);


    /**
     * 为特定路径的节点 添加监听器（同时监听数据变化以及该节点存在性变化）
     *
     * @param path
     * @param handler
     */
    void addNodeDataChangedHandler(String path,
                                   DomiNodeHandler handler) throws KeeperException, InterruptedException;

    /**
     * 为特定路径的节点 添加监听器（监听数子节点数量变化）
     *
     * @param path
     * @param handler
     */
    void addChildrenChangedHandler(String path,
                                   DomiChildrenNodeHandler handler) throws KeeperException, InterruptedException;


    /**
     * 移除特定路径节点的监听器
     *
     * @param path
     */
    void removeNodeDataChangedHandler(String path);

    /**
     * 移除特定路径节点的子节点处理器
     *
     * @param path
     */
    void removeChildrenChangedHandler(String path);

    /**
     * 移除特定路径节点的所有处理器
     *
     * @param path
     */
    void removeHandlerByPath(String path);

    /**
     * 判断是否有 path 节点的子节点监听处理器
     *
     * @param path
     */
    boolean containChildrenChangedHandler(String path);

    /**
     * 判断是否有 path 节点的监听处理器
     *
     * @param path
     * @return
     */
    boolean containNodeDataChangedHandler(String path);

    /**
     * 重新注册所有节点监听器
     */
    void refreshAllNodeDataChangedHandlers();


    /**
     * 重新注册所有子节点监听器
     */
    void refreshAllChildrenChangedHandlers();

    /**
     * 发布非持久化节点
     *
     * @param path
     * @param value
     * @return 实际创建的节点路径
     * @throws KeeperException
     * @throws InterruptedException
     */
    String createEphemeralNode(String path, String value) throws KeeperException, InterruptedException;

    /**
     * 发布持久化节点
     *
     * @param path
     * @param value
     * @return 实际创建的节点路径
     * @throws KeeperException
     * @throws InterruptedException
     */
    String createNode(String path, String value) throws KeeperException, InterruptedException;

    /**
     * 删除节点
     *
     * @param path
     * @throws KeeperException
     * @throws InterruptedException
     */
    void deleteNode(String path) throws KeeperException, InterruptedException;


    /**
     * 递归删除节点
     *
     * @param path
     */
    void deleteNodeRecursively(String path) throws KeeperException, InterruptedException;

    /**
     * 判断节点是否存在
     *
     * @param path
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    boolean exists(String path) throws KeeperException, InterruptedException;

    /**
     * 判断节点是否存在
     *
     * @param path
     * @param watch 监听此节点create 、delete事件
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    boolean exists(String path, boolean watch) throws KeeperException, InterruptedException;


    /**
     * 创建节点。若父节点不存在则先创建父节点
     *
     * @param path
     * @param value
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    String createNodeRecursively(String path, String value) throws KeeperException, InterruptedException;

    /**
     * 创建节点信息，若该节点已存在则更新此节点数据
     *
     * @param path
     * @param value
     */
    void createOrUpdateNodeData(String path, String value) throws KeeperException, InterruptedException;

    /**
     * 更新node数据
     *
     * @param path
     * @param value
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    Stat updateNodeData(String path, String value) throws KeeperException, InterruptedException;


    /**
     * 更新node数据
     *
     * @param path
     * @param value
     * @param version
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    Stat updateNodeData(String path, String value, int version) throws KeeperException, InterruptedException;


    /**
     * 获取节点数据
     *
     * @param path
     * @return
     */
    String getData(String path) throws KeeperException, InterruptedException;


    /**
     * 获取节点数据
     *
     * @param path
     * @param stat
     * @return
     * @throws InterruptedException
     * @throws org.apache.zookeeper.KeeperException
     */
    String getData(String path, Stat stat) throws KeeperException, InterruptedException;

    /**
     * 获取节点数据
     *
     * @param path
     * @param watch true:监听此节点的数据变化
     * @return
     */
    String getData(String path, boolean watch) throws KeeperException, InterruptedException;

    /**
     * 获取节点数据
     *
     * @param path
     * @param watch true:监听此节点的数据变化
     * @param stat
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    String getData(String path, boolean watch, Stat stat) throws KeeperException, InterruptedException;

    /**
     * 获取path节点的子节点名称列表
     *
     * @param path
     * @return
     */
    List<String> getChildren(String path) throws KeeperException, InterruptedException;

    /**
     * 获取path节点的子节点名称列表
     *
     * @param path
     * @param watch 监听节点个数变化的事件
     * @return
     */
    List<String> getChildren(String path, boolean watch) throws KeeperException, InterruptedException;


    /**
     * 获取本机节点对象
     *
     * @return
     */
    MachineNode getLocalMachine();

    /**
     * 从ZK端获取本机节点对象
     *
     * @return
     */
    MachineNode getLocalMachineFromZk();


    /**
     * 追加节点监听器（初始化前调用有效）
     *
     * @param nodeHandlerMap
     */
    void setNodeDataChangedHandlerBeforeInit(Map<String/*节点路径名称*/, DomiNodeHandler> nodeHandlerMap);

    /**
     * 追加子节点监听器（初始化前调用有效）
     *
     * @param childrenNodeHandlerMap
     */
    void setChildrenChangedHandlerBeforeInit(
            Map<String/*节点路径名称*/, DomiChildrenNodeHandler> childrenNodeHandlerMap);

    /**
     * 下线本机节点
     */
    void offlineLocalMachineNode() throws KeeperException, InterruptedException;

    /**
     * 上线本机节点
     */
    void onlineLocalMachineNode() throws KeeperException, InterruptedException, IOException;

    /**
     * 判断本机节点是否已在zk上注册
     *
     * @return
     */
    boolean isLocalMachineOnline();


}
