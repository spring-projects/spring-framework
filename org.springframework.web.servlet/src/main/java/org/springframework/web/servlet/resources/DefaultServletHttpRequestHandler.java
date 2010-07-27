package org.springframework.web.servlet.resources;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;

/**
 * An {@link HttpRequestHandler} for serving static files using the Servlet container's "default" Servlet.
 * 
 * <p>This handler is intended to be used with a "/*" mapping when the {@link DispatcherServlet} is mapped to "/", thus 
 * overriding the Servlet container's default handling of static resources.  The mapping to this handler should generally 
 * be ordered as the last in the chain so that it will only execute when no other more specific mappings (i.e., to controllers) 
 * can be matched.  
 * 
 * <p>Requests are handled by forwarding through the {@link RequestDispatcher} obtained via the name specified through the 
 * {@code fileServletName} property.  In most cases, the {@code fileServletName} does not need to be set explicitly, as the 
 * handler checks at initialization time for the presence of the default Servlet of one of the known containers.  However, if 
 * running in a container where the default Servlet's name is not known, or where it has been customized via configuration, the 
 * {@code fileServletName} will need to be set explicitly.
 * 
 * @author Jeremy Grelle
 * @since 3.0.4
 */
public class DefaultServletHttpRequestHandler implements InitializingBean, HttpRequestHandler, ServletContextAware {

	/**
	 * Default Servlet name used by Tomcat, Jetty, JBoss, and Glassfish
	 */
	private static final String COMMON_DEFAULT_SERVLET_NAME = "default";
	
	/**
	 * Default Servlet name used by Resin
	 */
	private static final String RESIN_DEFAULT_SERVLET_NAME = "resin-file";
	
	/**
	 * Default Servlet name used by WebLogic
	 */
	private static final String WEBLOGIC_DEFAULT_SERVLET_NAME = "FileServlet";
	
	/**
	 * Default Servlet name used by WebSphere
	 */
	private static final String WEBSPHERE_DEFAULT_SERVLET_NAME = "SimpleFileServlet";
	
	private ServletContext servletContext;
	
	private String fileServletName;
	
	/**
	 * If the {@code filedServletName} property has not been explicitly set, attempts to locate the default Servlet using the 
	 * known common container-specific names.
	 */
	public void afterPropertiesSet() throws Exception {
		if (!StringUtils.hasText(this.fileServletName)) {
			if (this.servletContext.getNamedDispatcher(COMMON_DEFAULT_SERVLET_NAME) != null) {
				this.fileServletName = COMMON_DEFAULT_SERVLET_NAME;
			} else if (this.servletContext.getNamedDispatcher(RESIN_DEFAULT_SERVLET_NAME) != null) {
				this.fileServletName = RESIN_DEFAULT_SERVLET_NAME;
			} else if (this.servletContext.getNamedDispatcher(WEBLOGIC_DEFAULT_SERVLET_NAME) != null) {
				this.fileServletName = WEBLOGIC_DEFAULT_SERVLET_NAME;
			} else if (this.servletContext.getNamedDispatcher(WEBSPHERE_DEFAULT_SERVLET_NAME) != null) {
				this.fileServletName = WEBSPHERE_DEFAULT_SERVLET_NAME;
			}
			Assert.hasText(this.fileServletName, "Unable to locate the default servlet for serving static content.  Please set the 'fileServletName' property explicitly.");
		}		
	}
	
	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		RequestDispatcher rd = this.servletContext.getNamedDispatcher(this.fileServletName);
		Assert.notNull(rd, "A RequestDispatcher could not be located for the servlet name '"+this.fileServletName+"'");
		rd.forward(request, response);
	}
	
	/**
	 * Set the name of the default Servlet to be forwarded to for static resource requests.
	 * @param fileServletName The name of the Servlet to use for static resources.
	 */
	public void setDefaultServletName(String fileServletName) {
		this.fileServletName = fileServletName;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;		
	}
}