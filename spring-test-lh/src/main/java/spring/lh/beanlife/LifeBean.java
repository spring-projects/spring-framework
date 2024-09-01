package spring.lh.beanlife;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class LifeBean implements InitializingBean, BeanNameAware, DisposableBean, ApplicationContextAware {
	private int id;

	private String name;

	public LifeBean(int id, String name) {
		this.id = id;
		this.name = name;
		System.out.println("bean内部级别：调用构造函数");
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
		System.out.println("bean内部级别：属性注入 id");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		System.out.println("bean内部级别：属性注入 name");
	}

	@Override
	public void setBeanName(String name) {
		System.out.println("bean内部级别：调用_BeanNameAware.setBeanName() 方法");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		LifeBean lifeBean = (LifeBean) applicationContext.getBean("lifeBean");
		System.out.println(lifeBean);
		System.out.println("bean内部级别：调用_BeanNameAware.setBeanName() 方法");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("bean内部级别：调用_InitializingBean.afterPropertiesSet() 方法");
	}

	public void myInit() {
		System.out.println("bean内部级别：调用 init-method 方法");
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("bean内部级别：调用 DisposableBean.destroy() 方法");
	}

	public void myDestroy() {
		System.out.println("bean内部级别：调用 destroy-method 方法");
	}

	@Override
	public String toString() {
		return "LifeBean{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}
}