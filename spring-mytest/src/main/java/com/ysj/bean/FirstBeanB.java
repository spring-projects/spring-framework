package com.ysj.bean;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class FirstBeanB implements  EnvironmentAware{


	@Override
	public void setEnvironment(Environment environment) {

	}
}
