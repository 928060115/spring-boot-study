package com.company.project.core.service.AsyncService;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


/**
 * 异步任务示例
 * 单发服务模式
 * 多个服务之间逻辑上不存在相互依赖关系，执行先后顺序没有严格的要求，逻辑上可以被并行执行。对于单发服务只有请求，没有应答，很容易设计成异步的。发起服务调用后。立即返回，不需要同步阻塞等待应答。
 */
@Service
public class AsyncNoResponseService {

    @Async
    public void sendA() throws InterruptedException {
        System.out.println("send A============================");
        long startTime=System.currentTimeMillis();
        Thread.sleep(5000);
        long endTime=System.currentTimeMillis();
        System.out.println("A 耗时:"+(endTime-startTime));

    }

    @Async
    public void sendB() throws InterruptedException{
        System.out.println("send B============================");
        long startTime=System.currentTimeMillis();
        Thread.sleep(2000);
        long endTIme=System.currentTimeMillis();
        System.out.println("B 耗时:"+(endTIme-startTime));
    }
}
