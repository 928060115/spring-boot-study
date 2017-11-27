package com.company.project.core.service.zookeeper.watcher;

import com.company.project.core.service.zookeeper.ZkManager;

import java.util.List;

/**
 * 子节点变化处理器
 *
 * @author liwei
 *         16-10-27 下午1:39
 */
public interface DomiChildrenNodeHandler {


    /**
     * 节点变化(增加或删除节点)后的处理
     *
     * @param path
     * @param value
     */
     Object handleChildrenChanged(String path, List<String> value, ZkManager zkManager);
}
