package hzm;

import hzm.bean.DemoBean;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Hezeming
 * @version 1.0
 * @date 2020年10月26日
 */
public class SpringTest {

	@Test
	public void demo() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean("demoBean", DemoBean.class);
		context.refresh();

		final DemoBean bean = context.getBean(DemoBean.class);
		System.out.println(bean);
	}
}
