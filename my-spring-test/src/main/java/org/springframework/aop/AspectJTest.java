package org.springframework.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author sushuaiqiang
 * @date 2024/7/8 - 10:51
 */
@Aspect
public class AspectJTest {

	@Pointcut("execution(* *.test(..))")
	public void test(){

	}

	@Before("test()")
	public void beforeTest(){
		System.out.println("前置通知");
	}

	@After("test()")
	public void afterTest(){
		System.out.println("后置通知");
	}

	@Around("test()")
	public Object aroundTest(ProceedingJoinPoint p) {
		System.out.println("环绕前置");
		Object o = null;
		try {
			o = p.proceed();
		}catch (Throwable e){
			e.printStackTrace();
		}
		System.out.println("环绕后置");
		return o;
	}
}
