package com.cn;

import com.cn.mayf.IDemoTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.AnnotatedElement;

/**
 * @Author mayf
 * @Date 2021/3/7 13:51
 */
public class MainStart {
	public static void main(String[] args) {
		AppConfig config = new AppConfig();

		System.out.println(config.getClass());

		System.out.println(config.getClass() instanceof AnnotatedElement);
		System.out.println(MainStart.class instanceof AnnotatedElement);
	}
	public static void main111(String[] args) {
		AnnotationConfigApplicationContext ac =
				new AnnotationConfigApplicationContext(AppConfig.class);
//		ServiceDemo demo = ac.getBean(ServiceDemo.class);
//		demo.test();
//		System.out.println(()->test());
		MainStart start = new MainStart();

		start.test(start::retObj);
	}

	public void test(IDemoTest test){
		System.out.println(test.getObject());
	}

	public String retObj(){
		return "12312312";
	}

}
