package com.company.project.core.service.amqp;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 消息生产者例子
 */
@Service
public class Sender {
    @Resource
    private AmqpTemplate amqpTemplate;
    public void send(){
        System.out.println("发送消息中...");
        Date date=new Date();
        amqpTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME,"您好,现在的时间是"+date.toString());
    }
}
