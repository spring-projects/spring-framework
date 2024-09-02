package spring.lh.beanlife;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
	private static final Log log = LogFactory.getLog(MyBeanPostProcessor.class);

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			log.info("bean级别：调用 BeanPostProcessor.postProcessBeforeInitialization() 方法");
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if ("lifeBean".equals(beanName)) {
			log.info("bean级别：调用 BeanPostProcessor.postProcessAfterInitialization() 方法");
		}
		return bean;
	}
}