package com.lxcecho.aop.annoaop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 切面类
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Aspect // 切面类
@Component // ioc 容器
public class LogAspect {

	//设置切入点和通知类型
	//切入点表达式: execution(访问修饰符 增强方法返回类型 增强方法所在类全路径.方法名称(方法参数))
	//通知类型：
	// 前置 @Before(value="切入点表达式配置切入点")
	//@Before(value = "execution(* com.atguigu.spring6.aop.annoaop.CalculatorImpl.*(..))")
	@Before(value = "execution(public int com.atguigu.spring6.aop.annoaop.CalculatorImpl.*(..))")
	public void beforeMethod(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();
		System.out.println("Logger-->前置通知，方法名称：" + methodName + "，参数：" + Arrays.toString(args));
	}

	// 后置 @After()
	//@After(value = "com.atguigu.spring6.aop.annoaop.LogAspect.pointCut()")
	@After(value = "pointCut()")
	public void afterMethod(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->后置通知，方法名称：" + methodName);
	}

	// 返回 @AfterReturning
	@AfterReturning(value = "execution(* com.atguigu.spring6.aop.annoaop.CalculatorImpl.*(..))", returning = "result")
	public void afterReturningMethod(JoinPoint joinPoint, Object result) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->返回通知，方法名称：" + methodName + "，返回结果：" + result);
	}

	// 异常 @AfterThrowing 获取到目标方法异常信息
	//目标方法出现异常，这个通知执行
	@AfterThrowing(value = "execution(* com.atguigu.spring6.aop.annoaop.CalculatorImpl.*(..))", throwing = "ex")
	public void afterThrowingMethod(JoinPoint joinPoint, Throwable ex) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->异常通知，方法名称：" + methodName + "，异常信息：" + ex);
	}

	// 环绕 @Around()
	@Around("execution(* com.atguigu.spring6.aop.annoaop.CalculatorImpl.*(..))")
	public Object aroundMethod(ProceedingJoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();
		String argString = Arrays.toString(args);
		Object result = null;
		try {
			System.out.println("环绕通知==目标方法之前执行");

			//调用目标方法
			result = joinPoint.proceed();

			System.out.println("环绕通知==目标方法返回值之后");
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			System.out.println("环绕通知==目标方法出现异常执行");
		} finally {
			System.out.println("环绕通知==目标方法执行完毕执行");
		}
		return result;
	}

	//重用切入点表达式
	@Pointcut(value = "execution(* com.atguigu.spring6.aop.annoaop.CalculatorImpl.*(..))")
	public void pointCut() {
	}
}
