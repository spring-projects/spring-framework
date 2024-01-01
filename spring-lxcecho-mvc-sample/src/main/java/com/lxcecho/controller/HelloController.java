package com.lxcecho.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2024/1/1
 */
@RestController
public class HelloController {

	@GetMapping("/hello66")
	public String hello() {

		return "66666666~~~~~";
	}
}
