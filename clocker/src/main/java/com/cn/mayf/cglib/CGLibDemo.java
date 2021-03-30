package com.cn.mayf.cglib;

import org.springframework.cglib.proxy.MethodInterceptor;

/**
 * @Author mayf
 * @Date 2021/3/28 14:07
 */
public class CGLibDemo extends SourceClassDemo{
	MethodInterceptor interceptor;

	@Override
	public void cgLibMethod() {
		System.out.println(CGLibDemo.class+"#cgLibMethod()");
//		interceptor.intercept();
//		super.cgLibMethod();
	}
}
