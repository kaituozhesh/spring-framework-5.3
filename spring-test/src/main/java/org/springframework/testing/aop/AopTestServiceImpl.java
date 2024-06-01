package org.springframework.testing.aop;

import org.springframework.stereotype.Service;


public class AopTestServiceImpl implements AopTestService {

    @Override
    public void test(){
        System.out.println("asdf");
    }

}