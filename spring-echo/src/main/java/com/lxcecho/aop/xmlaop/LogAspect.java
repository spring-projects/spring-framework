package com.lxcecho.aop.xmlaop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 切面类
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Component //ioc容器
public class LogAspect {

	//前置通知
	public void beforeMethod(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();
		System.out.println("Logger-->前置通知，方法名称：" + methodName + "，参数：" + Arrays.toString(args));
	}

	// 后置通知
	public void afterMethod(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->后置通知，方法名称：" + methodName);
	}

	// 返回通知，获取目标方法的返回值
	public void afterReturningMethod(JoinPoint joinPoint, Object result) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->返回通知，方法名称：" + methodName + "，返回结果：" + result);
	}

	// 异常通知 获取到目标方法异常信息
	//目标方法出现异常，这个通知执行
	public void afterThrowingMethod(JoinPoint joinPoint, Throwable ex) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->异常通知，方法名称：" + methodName + "，异常信息：" + ex);
	}

	// 环绕通知
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
	@Pointcut(value = "execution(* com.lxcecho.aop.xmlaop.CalculatorImpl.*(..))")
	public void pointCut() {
	}
}
