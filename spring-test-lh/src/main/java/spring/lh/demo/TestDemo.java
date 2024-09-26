package spring.lh.demo;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * The type Test demo.
 * @author menglinghao
 */
public class TestDemo {
	/**
	 * The entry point of application.
	 *
	 * @param args the input arguments
	 */
	public static void main(String[] args) {
		System.out.println(9);
		AnnotationConfigApplicationContext an = new AnnotationConfigApplicationContext("classpath:applicationContext.xml");


		System.out.println(an);
	}
}
