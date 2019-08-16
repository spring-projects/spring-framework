package lc;

import lc.org.bean.LcCustomer;
import lc.org.beans.TestBean1th;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.ClassPathResource;

/**
 * @author : liuc
 * @date : 2019/7/9 19:50
 * @description :
 */
public class LcSpringTest {

	@Test
	public void testAop(){
		ApplicationContext context = new ClassPathXmlApplicationContext("lc/lc_test_aop_1th.xml");
		TestBean1th testBean1th = (TestBean1th) context.getBean("testBean1th");
		testBean1th.test();
		testBean1th.t();
		//System.out.println("====================");
		//testBean1th.test2();

	}

	@Test
	public void testBean(){
		//配合beans profiles属性
		//System.getProperties().setProperty("spring.profiles.active","test");
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("lc/lc_test_1th.xml"));
		TestBean1th tb = (TestBean1th) beanFactory.getBean("bean1th");
		tb.m("1");
		TestBean1th tb2 = (TestBean1th) beanFactory.getBean("testBean1th2");
		System.out.println(tb2.getAge());
		/*LcCustomer customer = (LcCustomer) beanFactory.getBean("customer");
		System.out.println(customer.getName()+"_"+customer.getHobby());*/
	}

	@Test
	public void testContext(){
		//记录: 原先在配置文件中设置了profile=test,没有设置系统变量的话,导致bean unavailable,是因为xml文件根本没有被解析
		ApplicationContext context = new ClassPathXmlApplicationContext("lc/lc_test_1th.xml");
		Object o = context.getBean("testBean1th");
		System.out.println(o);

	}

	public static void main(String[] args) {
		T t = new T();
		T2 t2 = new T2();
		final ThreadLocal tl = t.threadLocal;
		new Thread(() -> {
			t.setT(t);
			t.setName("lc2");
			System.out.println("t lc2: " + t.threadLocal.get());
			System.out.println("t lc2: " + (t.threadLocal == tl));
		}).start();
		t2.setT(t);
		t2.setName("lc");
		System.out.println("t2 lc: " + t.threadLocal.get());
		System.out.println("t2 lc: " + (t.threadLocal == t2.threadLocal));
		t.setT(t);
		t.setName("lc");
		System.out.println(t.t == t);
		System.out.println(t.t.t == t);
		System.out.println(t.threadLocal.get());
		System.out.println(t.threadLocal == tl);
	}

	static class T{
		private String name;
		private T t;
		private ThreadLocal<String> threadLocal = new NamedThreadLocal<>("T ThreadLocal");
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			threadLocal.set(name);
		}

		public T getT() {
			return t;
		}

		public void setT(T t) {
			this.t = t;
		}
	}

	static class T2{
		private String name;
		private T t;
		private ThreadLocal<String> threadLocal = new NamedThreadLocal<>("T ThreadLocal");
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			threadLocal.set(name);
		}

		public T getT() {
			return t;
		}

		public void setT(T t) {
			this.t = t;
		}
	}
}
