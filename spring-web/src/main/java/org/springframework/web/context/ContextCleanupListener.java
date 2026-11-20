/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context;

import java.util.Enumeration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;

/**
 * Web application listener that cleans up remaining disposable attributes
 * in the ServletContext, i.e. attributes which implement {@link DisposableBean}
 * and haven't been removed before. This is typically used for destroying objects
 * in "application" scope, for which the lifecycle implies destruction at the
 * very end of the web application's shutdown phase.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.context.support.ServletContextScope
 * @see ContextLoaderListener
 */
public class ContextCleanupListener implements ServletContextListener {

	private static final Log logger = LogFactory.getLog(ContextCleanupListener.class);


	@Override
	public void contextInitialized(ServletContextEvent event) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		cleanupAttributes(event.getServletContext());
	}


	/**
	 * Find all Spring-internal ServletContext attributes which implement
	 * {@link DisposableBean} and invoke the destroy method on them.
	 * @param servletContext the ServletContext to check
	 * @see DisposableBean#destroy()
	 */
	static void cleanupAttributes(ServletContext servletContext) {
		Enumeration<String> attrNames = servletContext.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = attrNames.nextElement();
			if (attrName.startsWith("org.springframework.")) {
				Object attrValue = servletContext.getAttribute(attrName);
				if (attrValue instanceof DisposableBean disposableBean) {
					try {
						disposableBean.destroy();
					}
					catch (Throwable ex) {
						if (logger.isWarnEnabled()) {
							logger.warn("Invocation of destroy method failed on ServletContext " +
									"attribute with name '" + attrName + "'", ex);
						}
					}
				}
			}
		}
	}

}
