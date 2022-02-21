package com.pengbin.spring.event;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 有两种方法可以创建监听者，一种是直接实现ApplicationListener的接口，一种是使用注解 @EventListener ， 注解是添加在监听方法上的
 */
@Component
public class ApplicationListenerTest implements ApplicationListener<TestEvent> {
	@Override
	public void onApplicationEvent(TestEvent event) {
		event.getMessage();
	}
}
