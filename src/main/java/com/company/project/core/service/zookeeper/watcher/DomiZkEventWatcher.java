package com.company.project.core.service.zookeeper.watcher;

import com.company.project.core.service.zookeeper.MachineNode;
import com.company.project.core.service.zookeeper.ZkManager;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * zk事件监视器
 *
 * @author qingren.lw
 *         14-9-12 下午5:29
 */
public class DomiZkEventWatcher implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(DomiZkEventWatcher.class);
    private ZkManager manager;
    private static boolean rootCreated = false;


    /**
     * 本机节点
     */
    private MachineNode localMachine;

    public DomiZkEventWatcher(ZkManager manager, MachineNode localMachine) {
        this.localMachine = localMachine;
        this.manager = manager;

    }

    /**
     * zk 事件处理器
     *
     * @param event
     */
    @Override
    public void process(WatchedEvent event) {
        logger.info("Received event:{}", event);

        if (event.getState() == Event.KeeperState.Expired) {
            //若ZK出现超时，则需重新初始化ZK连接
            try {
                logger.warn("Zookeeper session expired.Reconnecting...");
                processSessionExpired();

            } catch (IOException e) {
                logger.error("Re-connect zookeeper failed.", e);
                throw new RuntimeException(e);//此处抛个异常，为阻止后续流程
            }
        }
        if (event.getType() == Event.EventType.None) {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                processSyncConnected();
                logger.warn("Zookeeper connected.");
                //连接成功后，直接发起一个任务节点变化事件，以防止系统 down掉后，任务变成僵尸节点
                resumeEvent();
            }
            return;
        }

        manager.addEvent(event);

    }


    /**
     * 恢复节点监听事件
     */
    private void resumeEvent() {
        //此方法暂预留


        ////构造模型任务子节点变化事件
        //WatchedEvent event = new WatchedEvent(Event.EventType.NodeChildrenChanged, Event.KeeperState.SyncConnected,
        //        ZkManager.EBAY_MODEL_TASKS_ROOT_PATH);
        //manager.addEvent(event);
        ////构造批处理任务子节点变化
        //event = new WatchedEvent(Event.EventType.NodeChildrenChanged, Event.KeeperState.SyncConnected,
        //        ZkManager.EBAY_BATCH_TASKS_ROOT_PATH);
        //manager.addEvent(event);


    }

    /**
     * 会话超时处理:重新初始化zkManager
     */
    private void processSessionExpired() throws IOException {
        manager.init();
    }

    /**
     * 建立连接后的处理
     */
    private void processSyncConnected() {
        manager.countDownWaitForConnectedLatch();
        ObjectMapper om = new ObjectMapper();
        try {
            createRootNode();
            if (localMachine != null) {
                //若服务处于上线状态，则重新发布本机节点

                if(manager.isLocalMachineOnline()){
                    manager.onlineLocalMachineNode();
                }

                //String machineNodeValue = om.writeValueAsString(localMachine);
                ////发布非持久结点（自身）
                //String selfNodePath = ZkManager.TC_MACHINE_LIST_PATH + ZkManager.PATH_SPLITTER + localMachine.nodeName();
                //if (!manager.exists(selfNodePath)) {
                //    manager.createEphemeralNode(selfNodePath, machineNodeValue);
                //}
            }
            //刷新所有监听器
            manager.refreshAllChildrenChangedHandlers();
            manager.refreshAllNodeDataChangedHandlers();

        } catch (Exception e) {
            logger.error("Initializing node failed.", e);
        }

    }


    /**
     * 创建根节点
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void createRootNode() throws KeeperException, InterruptedException {
        if (rootCreated) {
            return;
        }

        synchronized (this.getClass()) {
            if (rootCreated) {
                return;
            }

            //发布各根结点
            if (!manager.exists(ZkManager.TC_ROOT_PATH)) {
                manager.createNode(ZkManager.TC_ROOT_PATH, "");
            }
            if (!manager.exists(ZkManager.TC_MACHINE_LIST_PATH)) {
                manager.createNode(ZkManager.TC_MACHINE_LIST_PATH, "");
            }
            rootCreated = true;
        }
    }


    public MachineNode getLocalMachine() {
        return localMachine;
    }

    public void setLocalMachine(MachineNode localMachine) {
        this.localMachine = localMachine;
    }
}
