package com.atlwj.demo.web.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class MyServletContainerInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	/**
	 *
	 * 获取跟容器的配置类  父容器
	 * @return
	 */
	@Override
	protected Class<?>[] getRootConfigClasses() {
		System.out.println("getRootConfigClasses....");
		return new Class<?>[]{RootConfig.class};
	}

	/**
	 *
	 * 获取web容器的配置类  子容器
	 * @return
	 */
	@Override
	protected Class<?>[] getServletConfigClasses() {
		System.out.println("getServletConfigClasses....");
		return new Class<?>[]{WebConfig.class};
	}

	/**
	 * 获取DispatcherServlet的映射信息
	 *   【/】拦截所有请求，但不包括*.jsp
	 *   【/*】连.jsp都拦截
	 * @return
	 */
	@Override
	protected String[] getServletMappings() {
		return new String[]{"/"};
	}
}
