/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.rx.web.servlet;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Arjen Poutsma
 */
public class HttpHandlerServletTomcatIntegrationTests extends AbstractHttpHandlerServletIntegrationTestCase {

	private static Tomcat tomcatServer;

	@BeforeClass
	public static void startServer() throws LifecycleException, InterruptedException {
		tomcatServer = new Tomcat();
		tomcatServer.setPort(port);
		File base = new File(System.getProperty("java.io.tmpdir"));
		Context rootCtx = tomcatServer.addContext("", base.getAbsolutePath());

		HttpHandlerServlet servlet = new HttpHandlerServlet();
		servlet.setHandler(new EchoHandler());

		tomcatServer.addServlet(rootCtx, "handlerServlet", servlet);
		rootCtx.addServletMapping("/rx", "handlerServlet");

		tomcatServer.start();
	}

	@AfterClass
	public static void stopServer() throws LifecycleException {
		tomcatServer.stop();
	}

}