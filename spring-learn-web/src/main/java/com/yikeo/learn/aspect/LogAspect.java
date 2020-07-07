package com.yikeo.learn.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class LogAspect {

	@Pointcut("execution(* *.demo(..))")
	public void test() {

	}
}
