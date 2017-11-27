package com.company.project.core.service.dubbo;

import com.alibaba.dubbo.config.annotation.Reference;


import com.company.project.Tester;
import org.junit.Test;


/**
 *
 */
public class DubboConsumerTest extends Tester{


    @Reference(version = "1.0.0")
    CityDubboService consumerService;

    @Test
    public void test(){
        City result=consumerService.findCityByName("杭州");
        System.out.println("==================================="+result.toString());
    }
}
