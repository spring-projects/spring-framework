package lc;

import lc.org.aop.AopBean;
import lc.org.aop.IAopBean;
import lc.org.beans.ITestBean;
import lc.org.beans.TestBean1th;
import org.junit.Test;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Enumeration;
import java.util.Properties;

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
		TestBean1th testBean1th2 = (TestBean1th) context.getBean("testBean1th");
		//testBean1th2.test2();
		System.out.println(testBean1th == testBean1th2);
		//System.out.println(testBean1th.getClass().isAssignableFrom());
		//testBean1th.t();
		//System.out.println("====================");
		//testBean1th.test2();
	}

	@Test
	public void testAop2(){
		XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("lc/lc_test_aop_1th2.xml"));
		AnnotationAwareAspectJAutoProxyCreator creator = new AnnotationAwareAspectJAutoProxyCreator();
		creator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(creator);
		//creator.setInterceptorNames("");
		ITestBean tb = (ITestBean) beanFactory.getBean("testBean1th");
		//DeclareParentsAdvisor
		IAopBean aopBean = (IAopBean) tb;
		aopBean.aopTes();
		//testBean1th.t();
		tb.test();
		Class[] cls = tb.getClass().getClasses();
		for (Class c:
			 cls) {
			System.out.println(c.getName());
		}
		//System.out.println("====================");
		//testBean1th.test2();
	}

	@Test
	public void testBean(){
		//配合beans profiles属性
		System.getProperties().setProperty("spring.profiles.active","test");
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("lc/lc_test_1th.xml"));
		TestBean1th tb = (TestBean1th) beanFactory.getBean("bean1th");
		tb.m("1");
		TestBean1th tb2 = (TestBean1th) beanFactory.getBean("testBean1th2");
		beanFactory.getBean("&testBean1th2");
		System.out.println(tb2.getAge());
		/*LcCustomer customer = (LcCustomer) beanFactory.getBean("customer");
		System.out.println(customer.getName()+"_"+customer.getHobby());*/
	}

	@Test
	public void testRmi(){
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

	public static void main(String[] args) throws Exception{

		Properties mappings =
				PropertiesLoaderUtils.loadAllProperties("META-INF/spring.handlers", null);
		Enumeration enumeration = mappings.propertyNames();
		while (enumeration.hasMoreElements()){
			Object o = enumeration.nextElement();
			System.out.println(o.toString());
		}

	}


}
