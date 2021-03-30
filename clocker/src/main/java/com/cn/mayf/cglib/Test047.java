package com.cn.mayf.cglib;

import com.cn.AppConfigProxy;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Method;

/**
 * @Author mayf
 * @Date 2021/3/28 11:27
 * 这一期关键点------>代理模式
 */
public class Test047 {
	public static void main(String[] args) throws IllegalAccessException, InstantiationException {
		AnnotationConfigApplicationContext aca
				= new AnnotationConfigApplicationContext();
		aca.register(AppConfigProxy.class);
		aca.refresh();
		/**
		 * @Bean===>可以自己控制对象产生的过程
		 * @Configuration可以让@Bean循环调用的方法失效
		 * 原因:方法被代理修改过了
		 * 加上@Configuration后AppConfigProxy成为CGLib代理对象
		 * 保证Spring中Bean的单例原则
		 */
		System.out.println(aca.getBean("appConfigProxy"));

		/**
		 * 将被@Configuration注解的Appconfig变为代理类
		 * ConfigurationClassPostProcessor#enhanceConfigurationClasses(beanFactory);
		 *
		 * CGLib基于继承来实现代理
		 * 需要关注的类：BeanMethodInterceptor
		 *
		 */
//		 testCGLib();
		/**
		 *
		 * why？--->beanClass == null
		 * BD为了描述一个类，@Bean的无法确定具体的类型，可能为动态代理对象，固为null
		 * 两个代理对象------implements MethodInterceptor
		 * BeanMethodInterceptor
		 * BeanFactoryAwareMethodInterceptor
		 *
		 * ConfigurationClassBeanDefinition
		 */
		System.out.println(aca.getBean("serviceDemo").getClass());
	}

	public static void testCGLib(){
		Enhancer enhancer = new Enhancer();
		/**
		 * 传入父类
		 * 继承父类方式实现代理
		 */
		enhancer.setSuperclass(CGLibDemo.class);
		/**
		 * Spring中使用CallBackFilter
		 */
		MethodInterceptor callBack = new MethodInterceptor() {
			@Override
			public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
				System.out.println("cglib---MethodInterceptor---before logic");
				Object o1 = methodProxy.invokeSuper(o, objects);
				System.out.println("cglib---MethodInterceptor---after logic");
				return o1;
			}
		};
		/**
		 * 具体的实现逻辑
		 */
		enhancer.setCallback(callBack);
//		enhancer.setCallbackType(callBack.getClass());
		SourceClassDemo demo = (SourceClassDemo) enhancer.create();

		demo.cgLibMethod();
	}
}
