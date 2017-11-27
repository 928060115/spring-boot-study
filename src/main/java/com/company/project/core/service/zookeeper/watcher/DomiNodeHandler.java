package com.company.project.core.service.zookeeper.watcher;

import com.company.project.core.service.zookeeper.ZkManager;

/**
 * zk数据节点处理器
 *
 * @author qingren.lw
 *         14-9-14 下午9:02
 */
public interface DomiNodeHandler {


    /**
     * 增加节点后的处理
     *
     * @param path
     * @param value
     * @return
     */
    Object handleNodeCreated(String path, String value, ZkManager zkManager);

    /**
     * 节点数据变化(修改节点数据)后的处理
     *
     * @param path
     * @param value
     * @return
     */
    Object handleDataChanged(String path, String value, ZkManager zkManager);

    /**
     * 删除节点后的处理
     *
     * @param path
     * @return
     */
    Object handleNodeDeleted(String path, ZkManager zkManager);

}
