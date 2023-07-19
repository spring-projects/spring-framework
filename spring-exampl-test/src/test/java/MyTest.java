import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.zcc.configs.BeanConfig;

import java.util.Arrays;

public class MyTest {

	@Test
	public void test0() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(factory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource("myBeanFactory.xml"));

		System.out.println(Arrays.toString(factory.getBeanDefinitionNames()));

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfig.class);
		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		System.out.println(Arrays.toString(beanDefinitionNames));

	}
}
