package depenceeach;

import com.cn.mayf.AppConfig048;
import com.cn.mayf.depenteach.DepentService01;
import com.cn.mayf.depenteach.DepentService02;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author mayf
 * @Date 2021/3/30 23:03
 */
public class Test048 {
	/**
	 * Spring默认支持(1)单例且是(2)setter注入的Bean循环依赖
	 *   本质是属性注入的问题
	 * Spring可以关闭循环依赖
	 *   1.beanFactory.setAllowCircularReferences(false);
	 * Bean生命周期(关注的流程)
	 *   1.推断构造方法
	 *   2.实例化一个对象
	 *   3.属性注入
	 *   4.生命周期回调初始化方法
	 *   5.aop代理
	 * BeanFactory中的getBean方法==>公用方法，后续会循环引用
	 */

	@Test
	public void test01(){
		AnnotationConfigApplicationContext aca = new AnnotationConfigApplicationContext();
		aca.register(AppConfig048.class);

//		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) aca.getBeanFactory();
//		beanFactory.setAllowCircularReferences(false);

		aca.refresh();

		DepentService02 bean = aca.getBean(DepentService02.class);
		System.out.println(bean);
		DepentService02 bean1 = (DepentService02) aca.getBean("service02");
		System.out.println(bean1);
		DepentService02 bean2 = (DepentService02) aca.getBean("getDepencyService02");
		System.out.println(bean2);
//		DepentService01 bean01 = (DepentService01) aca.getBean("depentService01");

//		System.out.println(bean.getService01());
	}
}
