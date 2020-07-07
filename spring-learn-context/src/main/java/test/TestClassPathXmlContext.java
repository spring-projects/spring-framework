package test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import test.service.PersonService;

public class TestClassPathXmlContext {
	public static void main(String[] args) {
		ApplicationContext ac = new ClassPathXmlApplicationContext("spring-context.xml");
		PersonService person = ac.getBean(PersonService.class);
		person.print();
	}
}
