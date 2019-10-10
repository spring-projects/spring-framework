package com.atlwj.demo.ioc.lifecycle;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 测试bean的生命周期
 *
 * @author LengWJ
 */
public class LifeCycleBean02{

	@Autowired
	LifeCycleBean lifeCycleBean;

	public void say02(){
		System.out.println(lifeCycleBean);
	}

}
