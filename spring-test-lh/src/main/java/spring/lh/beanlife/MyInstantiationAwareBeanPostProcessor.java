package spring.lh.beanlife;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
	private static final Log log = LogFactory.getLog(MyInstantiationAwareBeanPostProcessor.class);

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			log.info("InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation() 方法");
		}
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			LifeBean lifeBean = (LifeBean) bean;
			log.info("InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation() 方法");

			log.info(lifeBean);
		}
		return true;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			log.info("InstantiationAwareBeanPostProcessor.postProcessProperties() 方法");
		}
		return null;
	}
}