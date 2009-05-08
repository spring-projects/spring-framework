package org.springframework.jdbc.config;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@link NamespaceHandler} for JDBC configuration namespace.
 * @author Oliver Gierke
 */
public class JdbcNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		registerBeanDefinitionParser("embedded-database", new EmbeddedDatabaseBeanDefinitionParser());
	}
}
