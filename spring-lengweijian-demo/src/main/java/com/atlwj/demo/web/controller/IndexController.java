package com.atlwj.demo.web.controller;

import com.atlwj.demo.web.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class IndexController {

	@RequestMapping("/index")
	public String index() {
		System.out.println("index.....");
		return "index";
	}


	@GetMapping("/get")
	@ResponseBody
	public User getUSer(){
		return new User("lucy","20");
	}
}
