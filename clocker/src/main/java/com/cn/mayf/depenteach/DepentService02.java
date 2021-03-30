package com.cn.mayf.depenteach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * @Author mayf
 * @Date 2021/3/30 23:01
 */
@Component("service02")
@Primary
public class DepentService02 {
	public DepentService02() {
//		System.out.println(DepentService02.class);
//		System.out.println(DepentService02.class+"非显示通过setter注入");
//		System.out.println(service01);
	}

	@Autowired
	private DepentService01 service01;

	public DepentService01 getService01() {
		return service01;
	}

	public void setService01(DepentService01 service01) {
		System.out.println(service01.getClass()+"===>显示通过setter注入");
		this.service01 = service01;
	}
}
