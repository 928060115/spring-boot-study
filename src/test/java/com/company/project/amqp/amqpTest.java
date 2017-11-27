package com.company.project.amqp;

import com.company.project.core.service.amqp.Sender;
import com.company.project.Tester;
import org.junit.Test;
import javax.annotation.Resource;

public class amqpTest extends Tester {
    @Resource
    private Sender sender;
    @Test
    public  void test(){
        sender.send();
    }
}
