package com.company.project.core.service.amqp;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;

import javax.jms.Queue;
import javax.jms.Topic;

/**
 * 定义队列\\主题
 */
@Configuration
@EnableJms
public class ActiveMQConfig {
    /**
     * 定义点对点队列
     * @return
     */
    @Bean
    public Queue queue(){
        return  new ActiveMQQueue("sample.queue");
    }

    /**
     * 定义一个主题
     * @return
     */
    @Bean
    public Topic topic(){
        return new ActiveMQTopic("sample.topic");
    }
}
