package com.ysj.bean;

import org.springframework.stereotype.Component;

@Component
public class FirstBeanAwareTest implements FirstBean{

	private FirstBeanA firstBeanA;
	private FirstBeanB firstBeanB;

	public FirstBeanA getFirstBeanA() {
		return firstBeanA;
	}

	public void setFirstBeanA(FirstBeanA firstBeanA) {
		this.firstBeanA = firstBeanA;
	}

	public FirstBeanB getFirstBeanB() {
		return firstBeanB;
	}

	@Override
	public void setFirstBeanB(FirstBeanB beanB) {
		this.firstBeanB = beanB;
	}
}
