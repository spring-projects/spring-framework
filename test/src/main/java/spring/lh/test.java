package spring.lh;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class test {
	public static void main(String[] args) {
		System.out.println(9);
		AnnotationConfigApplicationContext an = new AnnotationConfigApplicationContext("");
		System.out.println(an);
	}
}
