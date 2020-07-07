package com.yikeo.learn.controller;

import com.yikeo.learn.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

	@Autowired
	DemoService demoService;

	@RequestMapping("")
	public String index() {
		return "index";
	}

	@RequestMapping("home")
	public String home() {
		return "index";
	}

	@RequestMapping("demo")
	public String demo(String name, Model model) {
		model.addAttribute("msg", demoService.demo(name));
		return "demo";
	}
}
