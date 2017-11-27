package com.company.project.async;

import com.company.project.Tester;
import com.company.project.core.service.AsyncService.AsyncNoResponseService;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.IOException;

public class asyncNoResponseTest extends Tester {
    @Resource
    private AsyncNoResponseService asyncNoResponseService;
    @Test
    public void test() throws InterruptedException, IOException {
        asyncNoResponseService.sendA();
        asyncNoResponseService.sendB();
        System.in.read();//防止程序结束


        /*
        输出
        send A============================
        send B============================
        B 耗时:2000
        A 耗时:5000
        * */
    }
}
