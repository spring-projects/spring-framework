package spring.lh.annotation.configuration;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.lh.annotation.configuration.bean.Person;
import spring.lh.annotation.configuration.config.ConfigAnnotation;

public class Test {
	public static void main(String[] args) {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConfigAnnotation.class);
		AnnotationConfigApplicationContext contextBank = new AnnotationConfigApplicationContext("");
		ConfigAnnotation bean = context.getBean(ConfigAnnotation.class);
		Person p1 = bean.person();
		Person p2 = bean.person();
		System.out.println(p2 == p1);
	}
}
