package com.company.project.core.service.amqp;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class Receiver {
    @Resource
    private AmqpTemplate amqpTemplate;
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receive(String message){
        System.out.println("Receive:"+message);
    }
}
