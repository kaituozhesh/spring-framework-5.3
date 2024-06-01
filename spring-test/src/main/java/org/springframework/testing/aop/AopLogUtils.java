package org.springframework.testing.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

public class AopLogUtils {

//
//
//    @Around(value = "myPointCut()")
//    public Object doAround(ProceedingJoinPoint joinPoint)
//    {
//        int result = 0;
//        try {
//            System.out.println("doAround 前面执行");
//            Object proceed = joinPoint.proceed();
//            System.out.println("doAround 后面执行");
//        }catch (Throwable t){
//            System.out.println("doAround 出错");
//        }finally {
//            System.out.println("doAround finally执行");
//        }
//        return result;
//
//    }

	public void doBefore(JoinPoint joinPoint) {
		System.out.println("doBefore 执行");
	}
//
//    @After(value = "myPointCut()")
    public void doAfter(JoinPoint joinPoint)
    {
        System.out.println("doAfter 执行");
    }
//
//
//    @AfterReturning(value = "myPointCut()",returning = "jsonResult")
//    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult)
//    {
//        System.out.println("doAfterReturning 执行");
//    }
//
//
//    @AfterThrowing(value = "myPointCut()", throwing = "e")
//    public void doAfterThrowing(JoinPoint joinPoint, Exception e)
//    {
//        System.out.println("AfterThrowing 执行");
//    }
}