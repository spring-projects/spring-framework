package com.atlwj.demo.aop.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import java.util.Arrays;


/**
 * 常见的Aspectj  PCD表达式：
 * 						1) The execution of any public method:
 * 							execution(public * *(..))
 * 						2) The execution of any method with a name that begins with set:
 * 							execution(* set*(..))
 * 					    3) The execution of any method defined by the AccountService interface:
 * 					    	execution(* com.xyz.service.AccountService.*(..))
 * 					    4) The execution of any method defined in the service package:
 * 					    	execution(* com.xyz.service.*.*(..))
 * 					    5) The execution of any method defined in the service package or one of its sub-packages:
 * 					    	execution(* com.xyz.service..*.*(..))
 * 					    6) Any join point (method execution only in Spring AOP) within the service package:
 * 					    	within(com.xyz.service.*)
 * 					    7) Any join point (method execution only in Spring AOP) within the service package or one of its sub-packages:
 * 					    	within(com.xyz.service..*)
 * 					    8) Any join point (method execution only in Spring AOP) where the proxy implements the AccountService interface:
 * 					    	this(com.xyz.service.AccountService)
 */
@Aspect
@Component
public class LogPrintAspect {

	@Pointcut("execution(public * *(..))")
	public void pointCut(){

	}

	@Before(value = "pointCut()")
	public void before(JoinPoint joinPoint){
		System.out.printf("方法：%s 执行之前打印，参数列表为：%s",joinPoint.getSignature().getName(), Arrays.asList(joinPoint.getArgs()));
	}

	@AfterReturning(value = "pointCut()",returning = "returnvalue")
	public void returnValue(JoinPoint joinPoint,Object returnvalue){
		System.out.printf("方法：%s 执行结束，返回值为：%s",joinPoint.getSignature().getName(),returnvalue);
	}

	@AfterThrowing(value = "pointCut()",throwing="ex")
	public void exceptionReturn(JoinPoint joinPoint,Exception ex){
		System.out.printf("方法：%s 执行结束，异常信息为：%s",joinPoint.getSignature().getName(),ex.getMessage());
	}

	@Around(value = "pointCut()")
	public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		System.out.println("before.....around....");
		Object proceed = proceedingJoinPoint.proceed();
		System.out.println("after.....around....");
		return proceed;
	}
}
