package initbean;

import aspectdemo.AppConfigAspect;
import aspectdemo.PersonService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;

public class InitMain {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfigInit.class);
		System.out.println(Arrays.asList(context.getBeanFactory().getBeanDefinitionNames()).toString().replaceAll(",", "\n"));

		Person person = context.getBean(Person.class);
		System.out.println(person.getName());
		context.close();
	}
}
