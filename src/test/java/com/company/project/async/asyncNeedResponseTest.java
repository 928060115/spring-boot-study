package com.company.project.async;

import com.company.project.Tester;
import com.company.project.core.service.AsyncService.AsyncNeedResponseService;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.concurrent.Future;

public class asyncNeedResponseTest extends Tester{

    @Resource
    private AsyncNeedResponseService asyncNeedResponseService;


    @Test
    public void test() throws Exception{
        long startTime=System.currentTimeMillis();

        Future<String> task1=asyncNeedResponseService.sendA();
        Future<String> task2=asyncNeedResponseService.sendB();

        while(true){
            if(task1.isDone() && task2.isDone()){
                break;
            }
        }

        long endTime=System.currentTimeMillis();
        System.out.println("总耗时:"+(endTime-startTime));

        /*输出
        send B
        send A
        耗时：2000
        耗时：2000
        总耗时:2019
        * */
    }

}
