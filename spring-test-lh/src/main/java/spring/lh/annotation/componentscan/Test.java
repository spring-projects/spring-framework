package spring.lh.annotation.componentscan;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.lh.annotation.componentscan.config.ComponentScanConfig;

import java.util.Arrays;

/**
 * The type Test.
 * @author menglinghao
 */
public class Test {
	/**
	 * The entry point of application.
	 *
	 * @param args the input arguments
	 */
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ComponentScanConfig.class);
 		String[] beanDefinitionNames = context.getBeanDefinitionNames();
		Arrays.stream(beanDefinitionNames).forEach(System.out::println);
	}
}
