package spring.lh.beanlife;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			System.out.println("bean级别：调用 BeanPostProcessor.postProcessBeforeInitialization() 方法");
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			System.out.println("bean级别：调用 BeanPostProcessor.postProcessAfterInitialization() 方法");
		}
		return bean;
	}
}