package com.spring.research.embed.tomcat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class EmbedTomcatController {

	@RequestMapping("/hello/test")
	@ResponseBody
	public String helloWorld() {
		return "hello world";
	}
}
