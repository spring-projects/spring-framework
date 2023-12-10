package com.lxcecho.iocxml;

import com.alibaba.druid.pool.DruidDataSource;
import com.lxcecho.iocxml.bean.*;
import com.lxcecho.iocxml.controller.UserController;
import com.lxcecho.iocxml.dao.UserDao;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class IocByXmlTest {

	@Test
	public void testBookSetter() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-di.xml");
		Book book = context.getBean("book", Book.class);
		System.out.println(book);
	}

	@Test
	public void testBookConstructor() {
		ApplicationContext context =
				new ClassPathXmlApplicationContext("bean-di.xml");
		Book book = context.getBean("bookCon", Book.class);
		System.out.println(book);
	}

	@Test
	public void testDI() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-dilist.xml");
		//员工对象
		Dept dept = context.getBean("dept", Dept.class);
		dept.info();

		/*ApplicationContext context =
				new ClassPathXmlApplicationContext("bean-diarray.xml");
		//员工对象
		Emp emp = context.getBean("emp", Emp.class);
		emp.work();*/
	}

	@Test
	public void testStu() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-diref.xml");
		Student student = context.getBean("studentp", Student.class);
		student.run();
	}

	@Test
	public void testUserDao() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean.xml");
		//根据类型获取接口对应bean
		UserDao userDao = context.getBean(UserDao.class);
		System.out.println(userDao);
		userDao.run();
	}

	@Test
	public void testJdbc() {
		/*DruidDataSource dataSource = new DruidDataSource();
		dataSource.setUrl("jdbc:mysql://localhost:3306/spring?serverTimezone=UTC");
		dataSource.setUsername("root");
		dataSource.setPassword("Amecho00#");
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");*/

		ApplicationContext context =
				new ClassPathXmlApplicationContext("bean-jdbc.xml");
		DruidDataSource dataSource = context.getBean(DruidDataSource.class);
		System.out.println(dataSource.getUrl());
	}

	//创建Logger对象
	private Logger logger = LoggerFactory.getLogger(IocByXmlTest.class);

	@Test
	public void testUserObject() {
		//加载spring配置文件，对象创建
		ApplicationContext context = new ClassPathXmlApplicationContext("bean.xml");

		//获取创建的对象
		User user = (User)context.getBean("user");
		System.out.println("1:"+user);

		//使用对象调用方法进行测试
		System.out.print("2:");
		user.add();

		//手动写日志
		logger.info("### 执行调用成功了..");
	}

	//反射创建对象
	@Test
	public void testUserObject1() throws Exception {
		//获取类Class对象
		Class clazz = Class.forName("com.lxcecho.iocxml.bean.User");
		//调用方法创建对象
		//Object o = clazz.newInstance();
		User user = (User)clazz.getDeclaredConstructor().newInstance();
		System.out.println(user);
	}

	@Test
	public void testBeanAuto() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-auto.xml");
		UserController controller = context.getBean("userController", UserController.class);
		controller.addUser();
	}

	@Test
	public void testBeanLife() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("bean-life.xml");
		User user = context.getBean("user", User.class);
		System.out.println("6 bean对象创建完成了，可以使用了");
		System.out.println(user);
		context.close(); //销毁
	}

	@Test
	public void testFactoryBean() {
		ApplicationContext context =
				new ClassPathXmlApplicationContext("bean-factorybean.xml");
		User user = (User) context.getBean("user");
		System.out.println(user);
	}

	@Test
	public void testBeanScope() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-scope.xml");
		Orders orders = context.getBean("orders", Orders.class);
		System.out.println(orders);
		Orders orders1 = context.getBean("orders", Orders.class);
		System.out.println(orders1);
	}

}
