package com.pengbin.spring.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class InitialingzingBeanTest implements InitializingBean {
	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitializingBean..");
	}
}
