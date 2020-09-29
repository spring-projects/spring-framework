package io.codegitz.customtag;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.stream.Stream;

/**
 * @author 张观权
 * @date 2020/9/9 17:28
 **/
public class BeanInfoDemo {
	public static void main(String[] args) throws IntrospectionException {
		BeanInfo beanInfo = Introspector.getBeanInfo(User.class);
		Stream.of(beanInfo.getPropertyDescriptors()).forEach(System.out::println);
	}
}
