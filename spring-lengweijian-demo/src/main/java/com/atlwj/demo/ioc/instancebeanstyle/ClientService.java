package com.atlwj.demo.ioc.instancebeanstyle;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ClientService {

	private static ClientService clientService = new ClientService();

	private ClientService() {}

	public static ClientService createInstance() {
		return clientService;
	}

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ioc = new ClassPathXmlApplicationContext("instancebeanstyle.xml");
		ClientService clientService = (ClientService) ioc.getBean("clientService");
		System.out.println(clientService);
	}
}