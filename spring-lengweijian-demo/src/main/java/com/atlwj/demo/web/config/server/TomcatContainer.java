package com.atlwj.demo.web.config.server;

import com.atlwj.demo.web.MyApp;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * @author lengweijian
 */
public class TomcatContainer implements WebContainer {

	private static final String APP_CLASSES_PATH = "/WEB-INF/classes";

	private Integer port;

	private String contextPath;

	private Tomcat tomcat;

	public TomcatContainer(Integer port,String contextPath){
		this.port = (port != null ? port : 8080);
		this.contextPath = contextPath != null ? contextPath : "";
		this.tomcat = getTomcat(port);
	}

	private Tomcat getTomcat(Integer port) {
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(port);
		return tomcat;
	}

	@Override
	public void init() {
		// TODO
	}

	@Override
	public void run() {
		setResource(tomcat);
		try {
			startAndAwait(tomcat);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			tomcat.stop();
		} catch (LifecycleException e) {
			e.printStackTrace();
		}
	}

	private void startAndAwait(Tomcat tomcat) throws LifecycleException {
		tomcat.start();
		tomcat.getServer().await();
	}

	private void setResource(Tomcat tomcat) {
		String sourcePath = MyApp.class.getResource("/").getPath();
		tomcat.getHost().setAutoDeploy(false);
		Context ctx = tomcat.addWebapp(contextPath, new File("src/main/").getAbsolutePath());
		WebResourceRoot resource = new StandardRoot(ctx);
		resource.addPreResources(new DirResourceSet(resource,APP_CLASSES_PATH,sourcePath,"/"));
		ctx.setResources(resource);
	}


}
