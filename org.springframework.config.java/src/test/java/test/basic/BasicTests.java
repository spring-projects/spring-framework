package test.basic;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.process.ConfigurationPostProcessor;
import org.springframework.config.java.util.DefaultScopes;

import test.beans.ITestBean;
import test.beans.TestBean;

public class BasicTests {
	@Test
	public void test() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("config", rootBeanDefinition(Config.class).getBeanDefinition());
		
		Config config = factory.getBean("config", Config.class);
		assertThat(config, notNullValue());
	}
	
	@Test
	public void test2() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("config", rootBeanDefinition(Config.class).getBeanDefinition());
		
		BeanFactoryPostProcessor bfpp = new MyPostProcessor();
		
		bfpp.postProcessBeanFactory(factory);
		
		OtherConfig config = factory.getBean("config", OtherConfig.class);
		assertThat(config, notNullValue());
	}
	
	@Test
	public void test3() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("config", rootBeanDefinition(Config.class).getBeanDefinition());
		
		new ConfigurationPostProcessor().postProcessBeanFactory(factory);
		
		String stringBean = factory.getBean("stringBean", String.class);
		
		assertThat(stringBean, equalTo("foo"));
	}
	
	@Test
	public void test4() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("config", rootBeanDefinition(Config2.class).getBeanDefinition());
		
		new ConfigurationPostProcessor().postProcessBeanFactory(factory);
		
		TestBean foo = factory.getBean("foo", TestBean.class);
		ITestBean bar = factory.getBean("bar", ITestBean.class);
		ITestBean baz = factory.getBean("baz", ITestBean.class);
		
		assertThat(foo.getSpouse(), sameInstance(bar));
		assertThat(bar.getSpouse(), not(sameInstance(baz)));
	}
}

class MyPostProcessor implements BeanFactoryPostProcessor {

	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinition beanDefinition = beanFactory.getBeanDefinition("config");
		beanDefinition.setBeanClassName(OtherConfig.class.getName());
	}
	
}

@Configuration
class Config {
	public @Bean String stringBean() {
		return "foo";
	}
}

@Configuration
class Config2 {
	public @Bean TestBean foo() {
		TestBean foo = new TestBean("foo");
		foo.setSpouse(bar());
		return foo;
	}
	
	public @Bean TestBean bar() {
		TestBean bar = new TestBean("bar");
		bar.setSpouse(baz());
		return bar;
	}
	
	@Bean(scope=DefaultScopes.PROTOTYPE) 
	public TestBean baz() {
		return new TestBean("bar");
	}
}

class OtherConfig {
}
