package spring.lh.annotation.configuration;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.lh.annotation.configuration.bean.Person;
import spring.lh.annotation.configuration.config.ConAnnotation;

public class Test {
	public static void main(String[] args) {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConAnnotation.class);
		ConAnnotation bean = context.getBean(ConAnnotation.class);
		Person p1 = bean.person();
		Person p2 = bean.person();
		System.out.println(p2 == p1);
	}
}
