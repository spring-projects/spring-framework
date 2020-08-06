package io.codegitz.service.impl;

import io.codegitz.service.HelloService;

/**
 * @author 张观权
 * @date 2020/8/6 19:47
 **/
public class HelloServiceImpl implements HelloService {
	@Override
	public String sayHello(String name) {
		System.out.println("hello: "+ name);
		return "success";
	}
}
