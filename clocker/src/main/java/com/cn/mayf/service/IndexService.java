package com.cn.mayf.service;

import org.springframework.stereotype.Service;

/**
 * @Author mayf
 * @Date 2021/3/9 12:52
 */
@Service(value = "aa")
public class IndexService {

	public IndexService() {
	}

	/**
	 * 1.beanService==null----->没有自动装配
	 * 2.beanService!=null----->不正常！
	 * 结论：有唯一的有参的构造方法,值被自动注入了
	 * @param beanService
	 */
	public IndexService(BeanService beanService) {
		System.out.println(beanService);
	}

	private String name;

	public void setName(String name) {
		this.name = name;
	}
	//	public IndexService(String str) {
//		System.out.println(str);
//	}

	public void query(){
		System.out.println("logic!");
	}
}
