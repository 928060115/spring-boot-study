package com.company.project.core.service.amqp;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    @JmsListener(destination = "sample.queue")
    public void receiveQueue(String text){
        System.out.println("消费者：来源于生产者的消息："+text);
    }

    @JmsListener(destination = "sample.topic")
    public void receiveSub1(String text){
        System.out.println("消费者：Consumer1="+text);
    }

    @JmsListener(destination = "sample.topic")
    public void receiveSub2(String text){
        System.out.println("消费者：Consumer2="+text);
    }
}
