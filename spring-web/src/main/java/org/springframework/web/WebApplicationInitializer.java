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

package org.springframework.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Interface to be implemented in Servlet 3.0+ environments in order to configure the
 * {@link ServletContext} programmatically -- as opposed to (or possibly in conjunction
 * with) the traditional {@code web.xml}-based approach.
 *
 * <p>Implementations of this SPI will be detected automatically by {@link
 * SpringServletContainerInitializer}, which itself is bootstrapped automatically
 * by any Servlet 3.0 container. See {@linkplain SpringServletContainerInitializer its
 * Javadoc} for details on this bootstrapping mechanism.
 *
 * <h2>Example</h2>
 * <h3>The traditional, XML-based approach</h3>
 * Most Spring users building a web application will need to register Spring's {@code
 * DispatcherServlet}. For reference, in WEB-INF/web.xml, this would typically be done as
 * follows:
 * <pre class="code">
 * &lt;servlet&gt;
 *   &lt;servlet-name>dispatcher&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;
 *     org.springframework.web.servlet.DispatcherServlet
 *   &lt;/servlet-class&gt;
 *   &lt;init-param>
 *     &lt;param-name>contextConfigLocation&lt;/param-name&gt;
 *     &lt;param-value>/WEB-INF/spring/dispatcher-config.xml&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 *   &lt;load-on-startup>1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;dispatcher&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;</pre>
 *
 * <h3>The code-based approach with {@code WebApplicationInitializer}</h3>
 * Here is the equivalent {@code DispatcherServlet} registration logic,
 * {@code WebApplicationInitializer}-style:
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      XmlWebApplicationContext appContext = new XmlWebApplicationContext();
 *      appContext.setConfigLocation("/WEB-INF/spring/dispatcher-config.xml");
 *
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(appContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * As an alternative to the above, you can also extend from {@link
 * org.springframework.web.servlet.support.AbstractDispatcherServletInitializer}.
 *
 * As you can see, thanks to Servlet 3.0's new {@link ServletContext#addServlet} method
 * we're actually registering an <em>instance</em> of the {@code DispatcherServlet}, and
 * this means that the {@code DispatcherServlet} can now be treated like any other object
 * -- receiving constructor injection of its application context in this case.
 *
 * <p>This style is both simpler and more concise. There is no concern for dealing with
 * init-params, etc, just normal JavaBean-style properties and constructor arguments. You
 * are free to create and work with your Spring application contexts as necessary before
 * injecting them into the {@code DispatcherServlet}.
 *
 * <p>Most major Spring Web components have been updated to support this style of
 * registration.  You'll find that {@code DispatcherServlet}, {@code FrameworkServlet},
 * {@code ContextLoaderListener} and {@code DelegatingFilterProxy} all now support
 * constructor arguments. Even if a component (e.g. non-Spring, other third party) has not
 * been specifically updated for use within {@code WebApplicationInitializers}, they still
 * may be used in any case. The Servlet 3.0 {@code ServletContext} API allows for setting
 * init-params, context-params, etc programmatically.
 *
 * <h2>A 100% code-based approach to configuration</h2>
 * In the example above, {@code WEB-INF/web.xml} was successfully replaced with code in
 * the form of a {@code WebApplicationInitializer}, but the actual
 * {@code dispatcher-config.xml} Spring configuration remained XML-based.
 * {@code WebApplicationInitializer} is a perfect fit for use with Spring's code-based
 * {@code @Configuration} classes. See @{@link
 * org.springframework.context.annotation.Configuration Configuration} Javadoc for
 * complete details, but the following example demonstrates refactoring to use Spring's
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext} in lieu of {@code XmlWebApplicationContext}, and
 * user-defined {@code @Configuration} classes {@code AppConfig} and
 * {@code DispatcherConfig} instead of Spring XML files. This example also goes a bit
 * beyond those above to demonstrate typical configuration of the 'root' application
 * context and registration of the {@code ContextLoaderListener}:
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      // Create the 'root' Spring application context
 *      AnnotationConfigWebApplicationContext rootContext =
 *        new AnnotationConfigWebApplicationContext();
 *      rootContext.register(AppConfig.class);
 *
 *      // Manage the lifecycle of the root application context
 *      container.addListener(new ContextLoaderListener(rootContext));
 *
 *      // Create the dispatcher servlet's Spring application context
 *      AnnotationConfigWebApplicationContext dispatcherContext =
 *        new AnnotationConfigWebApplicationContext();
 *      dispatcherContext.register(DispatcherConfig.class);
 *
 *      // Register and map the dispatcher servlet
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(dispatcherContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * As an alternative to the above, you can also extend from {@link
 * org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer}.
 *
 * Remember that {@code WebApplicationInitializer} implementations are <em>detected
 * automatically</em> -- so you are free to package them within your application as you
 * see fit.
 *
 * <h2>Ordering {@code WebApplicationInitializer} execution</h2>
 * {@code WebApplicationInitializer} implementations may optionally be annotated at the
 * class level with Spring's @{@link org.springframework.core.annotation.Order Order}
 * annotation or may implement Spring's {@link org.springframework.core.Ordered Ordered}
 * interface. If so, the initializers will be ordered prior to invocation. This provides
 * a mechanism for users to ensure the order in which servlet container initialization
 * occurs. Use of this feature is expected to be rare, as typical applications will likely
 * centralize all container initialization within a single {@code WebApplicationInitializer}.
 *
 * <h2>Caveats</h2>
 *
 * <h3>web.xml versioning</h3>
 * <p>{@code WEB-INF/web.xml} and {@code WebApplicationInitializer} use are not mutually
 * exclusive; for example, web.xml can register one servlet, and a {@code
 * WebApplicationInitializer} can register another. An initializer can even
 * <em>modify</em> registrations performed in {@code web.xml} through methods such as
 * {@link ServletContext#getServletRegistration(String)}. <strong>However, if
 * {@code WEB-INF/web.xml} is present in the application, its {@code version} attribute
 * must be set to "3.0" or greater, otherwise {@code ServletContainerInitializer}
 * bootstrapping will be ignored by the servlet container.</strong>
 *
 * <h3>Mapping to '/' under Tomcat</h3>
 * <p>Apache Tomcat maps its internal {@code DefaultServlet} to "/", and on Tomcat versions
 * &lt;= 7.0.14, this servlet mapping <em>cannot be overridden programmatically</em>.
 * 7.0.15 fixes this issue. Overriding the "/" servlet mapping has also been tested
 * successfully under GlassFish 3.1.<p>
 *
 * @author Chris Beams
 * @since 3.1
 * @see SpringServletContainerInitializer
 * @see org.springframework.web.context.AbstractContextLoaderInitializer
 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
 * @see org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer
 */
public interface WebApplicationInitializer {

	/**
	 * Configure the given {@link ServletContext} with any servlets, filters, listeners
	 * context-params and attributes necessary for initializing this web application. See
	 * examples {@linkplain WebApplicationInitializer above}.
	 * @param servletContext the {@code ServletContext} to initialize
	 * @throws ServletException if any call against the given {@code ServletContext}
	 * throws a {@code ServletException}
	 */
	void onStartup(ServletContext servletContext) throws ServletException;

}
