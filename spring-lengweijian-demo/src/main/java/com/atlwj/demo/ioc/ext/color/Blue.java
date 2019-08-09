package com.atlwj.demo.ioc.ext.color;

import org.springframework.stereotype.Component;

@Component
public class Blue {
	public Blue() {
		System.out.println("blue创建对象");
	}
}
