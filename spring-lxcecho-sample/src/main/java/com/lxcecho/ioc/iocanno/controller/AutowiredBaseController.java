package com.lxcecho.ioc.iocanno.controller;

import com.lxcecho.ioc.iocanno.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Controller
public class AutowiredBaseController {

	// 注入 service

	// 第一种方式 属性注入
    @Autowired // 根据类型找到对应对象，完成注入
    private BaseService baseService;

	// 第二种方式 set方法注入
    /*private BaseService baseService;
    @Autowired
    public void setBaseService(BaseService baseService) {
        this.baseService = baseService;
    }*/

	// 第三种方式  构造方法注入
    /*private BaseService baseService;
    @Autowired
    public AutowiredUserController(BaseService baseService) {
        this.baseService = baseService;
    }*/

	// 第四种方式 形参上注入
	/*private BaseService baseService;
	public AutowiredUserController(@Autowired BaseService baseService) {
		this.baseService = baseService;
	}*/

	// 第五种方式  只有一个有参数构造函数，无注解
	/*private final BaseService baseService;
	public AutowiredUserController(BaseService baseService) {
		this.baseService = baseService;
	}*/

	public void add() {
		System.out.println("AutowiredBaseController........");
		baseService.add();
	}
}
