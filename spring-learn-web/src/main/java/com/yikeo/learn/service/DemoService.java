package com.yikeo.learn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class DemoService {
	public String demo(String name) {
		return name + " is drinking, help!";
	}
}
