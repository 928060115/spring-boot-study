package com.company.project.amqp;

import com.company.project.Tester;
import com.company.project.core.service.amqp.Producer;
import org.junit.Test;
import javax.annotation.Resource;

public class amqpTest extends Tester {
    @Resource
    private Producer producer;
    @Test
    public  void test(){
        producer.send();
    }
}
