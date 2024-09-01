package spring.lh.beanlife;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BeanLifeCycleTest {
	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		LifeBean bean = (LifeBean) applicationContext.getBean("lifeBean");
		((AbstractApplicationContext) applicationContext).close();
	}
}