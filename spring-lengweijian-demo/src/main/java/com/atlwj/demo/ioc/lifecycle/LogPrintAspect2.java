package com.atlwj.demo.ioc.lifecycle;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
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
* 					    9) Any join point (method execution only in Spring AOP) that takes a single parameter and where the argument passed at runtime is Serializable
* 							args(java.io.Serializable)
* 					    10) Any join point (method execution only in Spring AOP) where the target object has a @Transactional annotation:
*                          @target(org.springframework.transaction.annotation.Transactional)
 *
 *                 Spring AOP APIs：
 *                 				Pointcut ： 一般使用切入点表达式更简单。
 *                 				ClassFilter
 *                 				MethodMatcher：用于测试此切入点是否与目标类上的给定方法匹配。
 *                 Advice Lifecycles:每个建议都是一个Spring bean。
 *
 *                 Advice Types in Spring:
 *                 				Interceptor
 *                 					---> MethodInterceptor
 *                 				BeforeAdvice
 *                 					---> MethodBeforeAdvice
 *                 				ThrowsAdvice
 *                 					----> RemoteThrowsAdvice
 *									----> ServletThrowsAdviceWithArguments
 *									----> CombinedThrowsAdvice
 *					After Returning Advice:
 *								Advice:
 *									----> AfterReturningAdvice
 *								AfterReturningAdvice:
 *					ProxyFactoryBean
 *					ProxyConfig: 这个类里面对spring aop的代码方式做了配置，通过设置properties key就可以设置：
 *							proxyTargetClass:true  ---- CGLIB proxies
 *							optimize 			   ----  控制是否将积极优化应用于通过CGLIB创建的代理。除非您完全了解相关AOP代理如何处理优化，否则您不应轻易使用此设置。目前仅用于CGLIB代理。它对JDK动态代理没有影响。
 *							frozen                 ----  此属性的默认值为false，因此允许更改（例如添加其他建议）
 *							exposeProxy 		   ----  确定当前代理是否应在ThreadLocal中公开，以便目标可以访问它。如果目标需要获取代理并且exposeProxy属性设置为true，则目标可以使用AopContext.currentProxy（）方法。
 *					6.4.4 Proxying Interfaces:
 *
 *
 *
 *
 *
**/
//@Aspect
//@Component
public class LogPrintAspect2 {

	@Pointcut("execution(* com.atlwj.demo.ioc.lifecycle.*.dis*(..))")
	public void pointCut(){

	}

	@Before(value = "pointCut()")
	public void before(JoinPoint joinPoint){
		System.out.printf("方法：%s 执行之前打印",joinPoint.getSignature().getName());
	}
//
//	@AfterReturning(value = "pointCut()",returning = "returnvalue")
//	public void returnValue(JoinPoint joinPoint,Object returnvalue){
//		System.out.printf("方法：%s 执行结束，返回值为：%s",joinPoint.getSignature().getName(),returnvalue);
//	}
//
//	@AfterThrowing(value = "pointCut()",throwing="ex")
//	public void exceptionReturn(JoinPoint joinPoint,Exception ex){
//		System.out.printf("方法：%s 执行结束，异常信息为：%s",joinPoint.getSignature().getName(),ex.getMessage());
//	}

//	@Around(value = "pointCut()")
//	public void around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
//		System.out.println("before.....around....");
//		Object proceed = proceedingJoinPoint.proceed();
//		System.out.println("after.....around....");
//		return;
//	}
}
