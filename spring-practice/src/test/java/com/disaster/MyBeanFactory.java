package com.disaster;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

import java.io.Serializable;

public class MyBeanFactory extends DefaultListableBeanFactory implements Serializable {

	private final static long serialVersionUID = 2323929392932323454l;

	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

	public MyBeanFactory() {
	}

	public MyBeanFactory(BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}

}
