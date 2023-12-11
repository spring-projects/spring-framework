package com.lxcecho;

import com.lxcecho.reflect.Car;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class CarTest {
	//1、获取Class对象多种方式
	@Test
	public void test01() throws Exception {
		//1 类名.class
		Class<?> clazz1 = Car.class;

		//2 对象.getClass()
		Class<?> clazz2 = new Car().getClass();

		//3 Class.forName("全路径")
		Class<?> clazz3 = Class.forName("com.lxcecho.reflect.Car");

		int length = clazz1.getInterfaces().length;
		System.out.println(length);
		//实例化
		Car car = (Car) clazz3.getConstructor().newInstance();
//        System.out.println(car);
	}

	//2、获取构造方法
	@Test
	public void test02() throws Exception {
		Class<?> clazz = Car.class;
		//获取所有构造
		// getConstructors()获取所有public的构造方法
//        Constructor[] constructors = clazz.getConstructors();
		// getDeclaredConstructors()获取所有的构造方法public  private
		Constructor[] constructors = clazz.getDeclaredConstructors();
		for (Constructor c : constructors) {
			System.out.println("方法名称：" + c.getName() + " 参数个数：" + c.getParameterCount());
		}

		//指定有参数构造创建对象
		//1 构造public
//        Constructor c1 = clazz.getConstructor(String.class, int.class, String.class);
//        Car car1 = (Car)c1.newInstance("夏利", 10, "红色");
//        System.out.println(car1);

		//2 构造private
		Constructor c2 = clazz.getDeclaredConstructor(String.class, int.class, String.class);
		c2.setAccessible(true);
		Car car2 = (Car) c2.newInstance("捷达", 15, "白色");
		System.out.println(car2);
	}

	//3、获取属性
	@Test
	public void test03() throws Exception {
		Class<?> clazz = Car.class;
		Car car = (Car) clazz.getDeclaredConstructor().newInstance();
		//获取所有public属性
		//Field[] fields = clazz.getFields();
		//获取所有属性（包含私有属性）
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (field.getName().equals("name")) {
				//设置允许访问
				field.setAccessible(true);
				field.set(car, "五菱宏光");

				System.out.println(car);
			}
			System.out.println(field.getName());
		}
	}

	//4、获取方法
	@Test
	public void test04() throws Exception {
		Car car = new Car("奔驰", 10, "黑色");
		Class<?> clazz = car.getClass();
		//1 public方法
		Method[] methods = clazz.getMethods();
		for (Method m1 : methods) {
			//System.out.println(m1.getName());
			//执行方法 toString
			if (m1.getName().equals("toString")) {
				String invoke = (String) m1.invoke(car);
				//System.out.println("toString执行了："+invoke);
			}
		}

		//2 private方法
		Method[] methodsAll = clazz.getDeclaredMethods();
		for (Method m : methodsAll) {
			//执行方法 run
			if (m.getName().equals("run")) {
				m.setAccessible(true);
				m.invoke(car);
			}
		}
	}
}
