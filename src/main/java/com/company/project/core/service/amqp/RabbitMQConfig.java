package com.company.project.core.service.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 连接配置,使用哪个队列
 */
@Configuration
public class RabbitMQConfig {
    public static final String QUEUE_NAME="spring-boot";
    @Bean
    public Queue queue(){
        return new Queue(QUEUE_NAME);
    }
}
