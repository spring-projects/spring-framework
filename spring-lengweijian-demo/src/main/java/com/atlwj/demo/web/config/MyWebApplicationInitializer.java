package com.atlwj.demo.web.config;

import org.apache.catalina.WebResourceRoot;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

public class MyWebApplicationInitializer implements WebApplicationInitializer {
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {

		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(WebConfig.class);
		context.refresh();

		DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
		ServletRegistration.Dynamic registration = servletContext.addServlet("xx", dispatcherServlet);
		registration.addMapping("/");
		registration.setLoadOnStartup(1);
		System.out.println("----------------");
	}
}
