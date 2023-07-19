package org.springframework.context.event.test.self_inject;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.AbstractApplicationContext;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class MyApplication {
	public static void main(String[] args) {
		try (AbstractApplicationContext context = new AnnotationConfigApplicationContext("org.springframework.context.event.test.self_inject")) {
			context.getBean(MyEventPublisher.class).publishMyEvent("hello");
			assert MyEventListener.eventCount == 1 : "event listener must fire exactly once";
		}
	}
}
