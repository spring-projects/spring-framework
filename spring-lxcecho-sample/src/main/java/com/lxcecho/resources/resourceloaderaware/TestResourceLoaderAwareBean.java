package com.lxcecho.resources.resourceloaderaware;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class TestResourceLoaderAwareBean implements ResourceLoaderAware {

	private ResourceLoader resourceLoader;

	/**
	 * 实现 ResourceLoaderAware 接口必须实现的方法
	 * 如果把该 Bean 部署在 Spring 容器中，该方法将会有 Spring 容器负责调用。Spring 容器调用该方法时，Spring 会将自身作为参数传给该方法。
	 *
	 * @param resourceLoader the ResourceLoader object to be used by this object
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 返回 ResourceLoader 对象的应用
	 *
	 * @return
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

}
