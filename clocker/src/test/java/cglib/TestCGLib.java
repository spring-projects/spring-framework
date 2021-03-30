package cglib;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.beans.ImmutableBean;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Author mayf
 * @Date 2021/3/29 23:18
 */
public class TestCGLib {

	@Test
	public void testImmutableBean(){
		SampleBean bean = new SampleBean();
		bean.setValue("Hello Bean!");
		SampleBean immutableBean = (SampleBean) ImmutableBean.create(bean);
		System.out.println(immutableBean.getValue());
		bean.setValue("next value!!!");
		System.out.println(immutableBean.getValue());
		immutableBean.setValue("next value!");
	}

	/**
	 * 动态生成Bean
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	@Test
	public void testBeanGenerator() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanGenerator generator = new BeanGenerator();
		generator.addProperty("value",String.class);
		Object myBean = generator.create();
		Method setterMethod = myBean.getClass().getMethod("setValue",String.class);
		setterMethod.invoke(myBean,"测试一段value!");
		Method getterMethod = myBean.getClass().getMethod("getValue");
		System.out.println(getterMethod.invoke(myBean));
	}
}
