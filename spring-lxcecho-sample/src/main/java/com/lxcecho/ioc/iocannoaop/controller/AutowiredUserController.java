package com.lxcecho.ioc.iocannoaop.controller;

import com.lxcecho.ioc.iocannoaop.service.UserService;
import org.springframework.stereotype.Controller;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Controller
public class AutowiredUserController {

	//注入service
	//第一种方式 属性注入
//    @Autowired //根据类型找到对应对象，完成注入
//    private UserService userService;

	//第二种方式 set方法注入
//    private UserService userService;
//
//    @Autowired
//    public void setUserService(UserService userService) {
//        this.userService = userService;
//    }

	//第三种方式  构造方法注入
//    private UserService userService;
//
//    @Autowired
//    public UserController(UserService userService) {
//        this.userService = userService;
//    }

	//第四种方式 形参上注入
//    private UserService userService;
//
//    public UserController(@Autowired UserService userService) {
//        this.userService = userService;
//    }

	//第五种方式  只有一个有参数构造函数，无注解
	private UserService userService;

	public AutowiredUserController(UserService userService) {
		this.userService = userService;
	}

	public void add() {
		System.out.println("controller........");
		userService.add();
	}
}
