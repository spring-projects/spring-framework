package spring.lh.beanlife;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component

public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
	private static final Log log = LogFactory.getLog(MyBeanFactoryPostProcessor.class);

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		log.info("beanFactory级别：BeanFactoryPostProcessor.postProcessBeanFactory() 方法");
	}
}