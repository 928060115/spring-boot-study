package com.company.project.core.service.zookeeper;

/**
 * 机器节点
 * @author jimersylee
 *
 */
public class MachineNode {
    private static final String SPLITTER = ":";

    /**
     * 机器名
     */
    private String hostname;

    /**
     * 当前服务的端口号
     */
    private int port;


    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 获取节点名称
     *
     * @return
     */
    public String nodeName() {
        return hostname + SPLITTER + port;
    }

}
