package com.spring.research.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DemoController {

	@RequestMapping("/hello/test")
	@ResponseBody
	public String helloWorld(){
		return "hello world";
	}
}
