/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.remoting.jaxws;

import java.net.InetSocketAddress;
import java.util.List;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceProvider;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple exporter for JAX-WS services, autodetecting annotated service beans
 * (through the JAX-WS {@link javax.jws.WebService} annotation) and exporting
 * them through the HTTP server included in Sun's JDK 1.6. The full address
 * for each service will consist of the server's base address with the
 * service name appended (e.g. "http://localhost:8080/OrderService").
 *
 * <p>Note that this exporter will only work on Sun's JDK 1.6 or higher, as well
 * as on JDKs that ship Sun's entire class library as included in the Sun JDK.
 * For a portable JAX-WS exporter, have a look at {@link SimpleJaxWsServiceExporter}.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see javax.jws.WebService
 * @see javax.xml.ws.Endpoint#publish(Object)
 * @see SimpleJaxWsServiceExporter
 * @deprecated as of Spring Framework 5.1, in favor of {@link SimpleJaxWsServiceExporter}
 */
@Deprecated
@org.springframework.lang.UsesSunHttpServer
public class SimpleHttpServerJaxWsServiceExporter extends AbstractJaxWsServiceExporter {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private HttpServer server;

	private int port = 8080;

	@Nullable
	private String hostname;

	private int backlog = -1;

	private int shutdownDelay = 0;

	private String basePath = "/";

	@Nullable
	private List<Filter> filters;

	@Nullable
	private Authenticator authenticator;

	private boolean localServer = false;


	/**
	 * Specify an existing HTTP server to register the web service contexts
	 * with. This will typically be a server managed by the general Spring
	 * {@link org.springframework.remoting.support.SimpleHttpServerFactoryBean}.
	 * <p>Alternatively, configure a local HTTP server through the
	 * {@link #setPort "port"}, {@link #setHostname "hostname"} and
	 * {@link #setBacklog "backlog"} properties (or rely on the defaults there).
	 */
	public void setServer(HttpServer server) {
		this.server = server;
	}

	/**
	 * Specify the HTTP server's port. Default is 8080.
	 * <p>Only applicable for a locally configured HTTP server.
	 * Ignored when the {@link #setServer "server"} property has been specified.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Specify the HTTP server's hostname to bind to. Default is localhost;
	 * can be overridden with a specific network address to bind to.
	 * <p>Only applicable for a locally configured HTTP server.
	 * Ignored when the {@link #setServer "server"} property has been specified.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Specify the HTTP server's TCP backlog. Default is -1,
	 * indicating the system's default value.
	 * <p>Only applicable for a locally configured HTTP server.
	 * Ignored when the {@link #setServer "server"} property has been specified.
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * Specify the number of seconds to wait until HTTP exchanges have
	 * completed when shutting down the HTTP server. Default is 0.
	 * <p>Only applicable for a locally configured HTTP server.
	 * Ignored when the {@link #setServer "server"} property has been specified.
	 */
	public void setShutdownDelay(int shutdownDelay) {
		this.shutdownDelay = shutdownDelay;
	}

	/**
	 * Set the base path for context publication. Default is "/".
	 * <p>For each context publication path, the service name will be
	 * appended to this base address. E.g. service name "OrderService"
	 * -> "/OrderService".
	 * @see javax.xml.ws.Endpoint#publish(Object)
	 * @see javax.jws.WebService#serviceName()
	 */
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	/**
	 * Register common {@link com.sun.net.httpserver.Filter Filters} to be
	 * applied to all detected {@link javax.jws.WebService} annotated beans.
	 */
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	/**
	 * Register a common {@link com.sun.net.httpserver.Authenticator} to be
	 * applied to all detected {@link javax.jws.WebService} annotated beans.
	 */
	public void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.server == null) {
			InetSocketAddress address = (this.hostname != null ?
					new InetSocketAddress(this.hostname, this.port) : new InetSocketAddress(this.port));
			HttpServer server = HttpServer.create(address, this.backlog);
			if (logger.isInfoEnabled()) {
				logger.info("Starting HttpServer at address " + address);
			}
			server.start();
			this.server = server;
			this.localServer = true;
		}
		super.afterPropertiesSet();
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebService annotation) {
		endpoint.publish(buildHttpContext(endpoint, annotation.serviceName()));
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation) {
		endpoint.publish(buildHttpContext(endpoint, annotation.serviceName()));
	}

	/**
	 * Build the HttpContext for the given endpoint.
	 * @param endpoint the JAX-WS Provider Endpoint object
	 * @param serviceName the given service name
	 * @return the fully populated HttpContext
	 */
	protected HttpContext buildHttpContext(Endpoint endpoint, String serviceName) {
		Assert.state(this.server != null, "No HttpServer available");
		String fullPath = calculateEndpointPath(endpoint, serviceName);
		HttpContext httpContext = this.server.createContext(fullPath);
		if (this.filters != null) {
			httpContext.getFilters().addAll(this.filters);
		}
		if (this.authenticator != null) {
			httpContext.setAuthenticator(this.authenticator);
		}
		return httpContext;
	}

	/**
	 * Calculate the full endpoint path for the given endpoint.
	 * @param endpoint the JAX-WS Provider Endpoint object
	 * @param serviceName the given service name
	 * @return the full endpoint path
	 */
	protected String calculateEndpointPath(Endpoint endpoint, String serviceName) {
		return this.basePath + serviceName;
	}


	@Override
	public void destroy() {
		super.destroy();
		if (this.server != null && this.localServer) {
			logger.info("Stopping HttpServer");
			this.server.stop(this.shutdownDelay);
		}
	}

}
