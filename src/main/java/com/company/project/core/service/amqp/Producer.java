package com.company.project.core.service.amqp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import javax.jms.Queue;
import javax.jms.Topic;

/**
 * 定义生产者
 */
@Configuration
public class Producer {

    @Autowired
    private  JmsMessagingTemplate jmsMessagingTemplate;
    @Autowired
    private Queue queue;
    @Autowired
    private Topic topic;





    /**
     * 每5s执行一次
     */
    @Scheduled(fixedRate = 5000,initialDelay = 3000)
    public void send(){
        /*//发送队列消息
        this.jmsMessagingTemplate.convertAndSend(this.queue,"生产者辛苦生产的点对点消息成果");
        System.out.println("生产者：生产的点对点消息成果");
        //发送订阅消息
        this.jmsMessagingTemplate.convertAndSend(this.topic,"生产者辛苦生产的订阅/发布消息成果");
        System.out.println("生产者：生产的订阅/发布消息成果");*/
    }
}
