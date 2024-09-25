package spring.lh.beanlife;

import org.springframework.beans.factory.FactoryBean;

public class MyFactoryBean implements FactoryBean {

	@Override
	public Object getObject() throws Exception {
		// 这里可以返回任意的bean、定制的bean。
		return null;
	}

	@Override
	public Class<?> getObjectType() {
		return null;
	}
}
