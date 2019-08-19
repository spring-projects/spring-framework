package com.atlwj.demo.ioc.resource;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.ComponentScan;

@Configurable
@ComponentScan(value = "com.atlwj.demo.ioc.resource")
public class ResourceApp {
	public static void main(String[] args) {



	}
}
