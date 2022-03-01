package springtest;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import springtest.aop.Performance;

/**
 * @author szd1007
 */
public class ContextTest {

	public static void main(String[] args) {
		ApplicationContext ctx = new FileSystemXmlApplicationContext("classpath:performer.xml");

		//xml config
// 		Performer performer = (Performer) ctx.getBean("duke");
//		performer.perform();
//		Performer p2 = (Performer) ctx.getBean("duke");
//		String [] names = ctx.getBeanNamesForType(Performer.class);
//		System.out.println(names.length);
//		System.out.println(names[0]);
//		System.out.println("isProtoType:" + ctx.isPrototype("duke"));
//		System.out`.println("isSingleton: " + ctx.isSingleton("duke"));
//        Park park = (Park) ctx.getBean("park");
//        Park park2 = (Park) ctx.getBean("park");
//        park.whoIsThere();
//        park2.whoIsThere();

        //aop>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		try {
			Performance juggler = (Performance) ctx.getBean("juggler");
			juggler.perform();
			System.out.println("系统测试");
		} catch (BeansException e) {
			e.printStackTrace();
		}

	}
}
