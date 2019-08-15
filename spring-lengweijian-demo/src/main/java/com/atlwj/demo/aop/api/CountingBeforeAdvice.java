package com.atlwj.demo.aop.api;

import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;


public class CountingBeforeAdvice implements MethodBeforeAdvice {
	private int count;

	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		++count;
	}

	public int getCount() {
		return count;
	}
}
