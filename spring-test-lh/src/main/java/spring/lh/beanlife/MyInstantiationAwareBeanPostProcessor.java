package spring.lh.beanlife;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			System.out.println("InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation() 方法");
		}
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			LifeBean lifeBean = (LifeBean) bean;
			System.out.println("InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation() 方法");
			System.out.println(lifeBean);
		}
		return true;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			System.out.println("InstantiationAwareBeanPostProcessor.postProcessProperties() 方法");
		}
		return null;
	}
}