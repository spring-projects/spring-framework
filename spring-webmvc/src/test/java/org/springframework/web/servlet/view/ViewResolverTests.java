/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;

import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockRequestDispatcher;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.theme.FixedThemeResolver;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 18.06.2003
 */
public class ViewResolverTests {

	@Test
	public void testBeanNameViewResolver() throws ServletException {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		MutablePropertyValues pvs1 = new MutablePropertyValues();
		pvs1.addPropertyValue(new PropertyValue("url", "/example1.jsp"));
		wac.registerSingleton("example1", InternalResourceView.class, pvs1);
		MutablePropertyValues pvs2 = new MutablePropertyValues();
		pvs2.addPropertyValue(new PropertyValue("url", "/example2.jsp"));
		wac.registerSingleton("example2", JstlView.class, pvs2);
		BeanNameViewResolver vr = new BeanNameViewResolver();
		vr.setApplicationContext(wac);
		wac.refresh();

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertEquals("Correct view class", InternalResourceView.class, view.getClass());
		assertEquals("Correct URL", "/example1.jsp", ((InternalResourceView) view).getUrl());

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "/example2.jsp", ((JstlView) view).getUrl());
	}

	@Test
	public void testUrlBasedViewResolverWithNullViewClass() {
		UrlBasedViewResolver resolver = new UrlBasedViewResolver();
		try {
			resolver.setViewClass(null);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void testUrlBasedViewResolverWithoutPrefixes() throws Exception {
		UrlBasedViewResolver vr = new UrlBasedViewResolver();
		vr.setViewClass(JstlView.class);
		doTestUrlBasedViewResolverWithoutPrefixes(vr);
	}

	@Test
	public void testUrlBasedViewResolverWithPrefixes() throws Exception {
		UrlBasedViewResolver vr = new UrlBasedViewResolver();
		vr.setViewClass(JstlView.class);
		doTestUrlBasedViewResolverWithPrefixes(vr);
	}

	@Test
	public void testInternalResourceViewResolverWithoutPrefixes() throws Exception {
		doTestUrlBasedViewResolverWithoutPrefixes(new InternalResourceViewResolver());
	}

	@Test
	public void testInternalResourceViewResolverWithPrefixes() throws Exception {
		doTestUrlBasedViewResolverWithPrefixes(new InternalResourceViewResolver());
	}

	private void doTestUrlBasedViewResolverWithoutPrefixes(UrlBasedViewResolver vr) throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		vr.setApplicationContext(wac);
		vr.setContentType("myContentType");
		vr.setRequestContextAttribute("rc");

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "example1", ((InternalResourceView) view).getUrl());
		assertEquals("Correct textContentType", "myContentType", ((InternalResourceView) view).getContentType());

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "example2", ((InternalResourceView) view).getUrl());
		assertEquals("Correct textContentType", "myContentType", ((InternalResourceView) view).getContentType());

		HttpServletRequest request = new MockHttpServletRequest(wac.getServletContext());
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		Map model = new HashMap();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, request, response);
		assertTrue("Correct tb attribute", tb.equals(request.getAttribute("tb")));
		assertTrue("Correct rc attribute", request.getAttribute("rc") instanceof RequestContext);

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertEquals("Correct view class", RedirectView.class, view.getClass());
		assertEquals("Correct URL", "myUrl", ((RedirectView) view).getUrl());
		assertSame("View not initialized as bean", wac, ((RedirectView) view).getApplicationContext());

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertEquals("Correct view class", InternalResourceView.class, view.getClass());
		assertEquals("Correct URL", "myUrl", ((InternalResourceView) view).getUrl());
	}

	private void doTestUrlBasedViewResolverWithPrefixes(UrlBasedViewResolver vr) throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		vr.setPrefix("/WEB-INF/");
		vr.setSuffix(".jsp");
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "/WEB-INF/example1.jsp", ((InternalResourceView) view).getUrl());

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "/WEB-INF/example2.jsp", ((InternalResourceView) view).getUrl());

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertEquals("Correct view class", RedirectView.class, view.getClass());
		assertEquals("Correct URL", "myUrl", ((RedirectView) view).getUrl());

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertEquals("Correct view class", InternalResourceView.class, view.getClass());
		assertEquals("Correct URL", "myUrl", ((InternalResourceView) view).getUrl());
	}

	@Test
	public void testInternalResourceViewResolverWithAttributes() throws Exception {
		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		Properties props = new Properties();
		props.setProperty("key1", "value1");
		vr.setAttributes(props);
		Map map = new HashMap();
		map.put("key2", new Integer(2));
		vr.setAttributesMap(map);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "example1", ((InternalResourceView) view).getUrl());
		Map attributes = ((InternalResourceView) view).getStaticAttributes();
		assertEquals("value1", attributes.get("key1"));
		assertEquals(new Integer(2), attributes.get("key2"));

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "example2", ((InternalResourceView) view).getUrl());
		attributes = ((InternalResourceView) view).getStaticAttributes();
		assertEquals("value1", attributes.get("key1"));
		assertEquals(new Integer(2), attributes.get("key2"));

		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		Map model = new HashMap();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, request, response);

		assertTrue("Correct tb attribute", tb.equals(request.getAttribute("tb")));
		assertTrue("Correct rc attribute", request.getAttribute("rc") == null);
		assertEquals("value1", request.getAttribute("key1"));
		assertEquals(new Integer(2), request.getAttribute("key2"));
	}

	@Test
	public void testInternalResourceViewResolverWithContextBeans() throws Exception {
		MockServletContext sc = new MockServletContext();
		final StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("myBean", TestBean.class);
		wac.registerSingleton("myBean2", TestBean.class);
		wac.setServletContext(sc);
		wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		Properties props = new Properties();
		props.setProperty("key1", "value1");
		vr.setAttributes(props);
		Map map = new HashMap();
		map.put("key2", new Integer(2));
		vr.setAttributesMap(map);
		vr.setExposeContextBeansAsAttributes(true);
		vr.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest(sc) {
			@Override
			public RequestDispatcher getRequestDispatcher(String path) {
				return new MockRequestDispatcher(path) {
					@Override
					public void forward(ServletRequest forwardRequest, ServletResponse forwardResponse) {
						assertTrue("Correct rc attribute", forwardRequest.getAttribute("rc") == null);
						assertEquals("value1", forwardRequest.getAttribute("key1"));
						assertEquals(new Integer(2), forwardRequest.getAttribute("key2"));
						assertSame(wac.getBean("myBean"), forwardRequest.getAttribute("myBean"));
						assertSame(wac.getBean("myBean2"), forwardRequest.getAttribute("myBean2"));
					}
				};
			}
		};
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		View view = vr.resolveViewName("example1", Locale.getDefault());
		view.render(new HashMap(), request, response);
	}

	@Test
	public void testInternalResourceViewResolverWithSpecificContextBeans() throws Exception {
		MockServletContext sc = new MockServletContext();
		final StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("myBean", TestBean.class);
		wac.registerSingleton("myBean2", TestBean.class);
		wac.setServletContext(sc);
		wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		Properties props = new Properties();
		props.setProperty("key1", "value1");
		vr.setAttributes(props);
		Map map = new HashMap();
		map.put("key2", new Integer(2));
		vr.setAttributesMap(map);
		vr.setExposedContextBeanNames(new String[] {"myBean2"});
		vr.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest(sc) {
			@Override
			public RequestDispatcher getRequestDispatcher(String path) {
				return new MockRequestDispatcher(path) {
					@Override
					public void forward(ServletRequest forwardRequest, ServletResponse forwardResponse) {
						assertTrue("Correct rc attribute", forwardRequest.getAttribute("rc") == null);
						assertEquals("value1", forwardRequest.getAttribute("key1"));
						assertEquals(new Integer(2), forwardRequest.getAttribute("key2"));
						assertNull(forwardRequest.getAttribute("myBean"));
						assertSame(wac.getBean("myBean2"), forwardRequest.getAttribute("myBean2"));
					}
				};
			}
		};
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		View view = vr.resolveViewName("example1", Locale.getDefault());
		view.render(new HashMap(), request, response);
	}

	@Test
	public void testInternalResourceViewResolverWithJstl() throws Exception {
		Locale locale = !Locale.GERMAN.equals(Locale.getDefault()) ? Locale.GERMAN : Locale.FRENCH;

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.addMessage("code1", locale, "messageX");
		wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		vr.setViewClass(JstlView.class);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "example1", ((JstlView) view).getUrl());

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "example2", ((JstlView) view).getUrl());

		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(locale));
		Map model = new HashMap();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, request, response);

		assertTrue("Correct tb attribute", tb.equals(request.getAttribute("tb")));
		assertTrue("Correct rc attribute", request.getAttribute("rc") == null);

		assertEquals(locale, Config.get(request, Config.FMT_LOCALE));
		LocalizationContext lc = (LocalizationContext) Config.get(request, Config.FMT_LOCALIZATION_CONTEXT);
		assertEquals("messageX", lc.getResourceBundle().getString("code1"));
	}

	@Test
	public void testInternalResourceViewResolverWithJstlAndContextParam() throws Exception {
		Locale locale = !Locale.GERMAN.equals(Locale.getDefault()) ? Locale.GERMAN : Locale.FRENCH;

		MockServletContext sc = new MockServletContext();
		sc.addInitParameter(Config.FMT_LOCALIZATION_CONTEXT, "org/springframework/web/context/WEB-INF/context-messages");
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.addMessage("code1", locale, "messageX");
		wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		vr.setViewClass(JstlView.class);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "example1", ((JstlView) view).getUrl());

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertEquals("Correct view class", JstlView.class, view.getClass());
		assertEquals("Correct URL", "example2", ((JstlView) view).getUrl());

		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(locale));
		Map model = new HashMap();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, request, response);

		assertTrue("Correct tb attribute", tb.equals(request.getAttribute("tb")));
		assertTrue("Correct rc attribute", request.getAttribute("rc") == null);

		assertEquals(locale, Config.get(request, Config.FMT_LOCALE));
		LocalizationContext lc = (LocalizationContext) Config.get(request, Config.FMT_LOCALIZATION_CONTEXT);
		assertEquals("message1", lc.getResourceBundle().getString("code1"));
		assertEquals("message2", lc.getResourceBundle().getString("code2"));
	}

	@Test
	public void testXmlViewResolver() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("testBean", TestBean.class);
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		TestBean testBean = (TestBean) wac.getBean("testBean");
		XmlViewResolver vr = new XmlViewResolver();
		vr.setLocation(new ClassPathResource("org/springframework/web/servlet/view/views.xml"));
		vr.setApplicationContext(wac);

		View view1 = vr.resolveViewName("example1", Locale.getDefault());
		assertTrue("Correct view class", TestView.class.equals(view1.getClass()));
		assertTrue("Correct URL", "/example1.jsp".equals(((InternalResourceView) view1).getUrl()));

		View view2 = vr.resolveViewName("example2", Locale.getDefault());
		assertTrue("Correct view class", JstlView.class.equals(view2.getClass()));
		assertTrue("Correct URL", "/example2new.jsp".equals(((InternalResourceView) view2).getUrl()));

		ServletContext sc = new MockServletContext();
		Map model = new HashMap();
		TestBean tb = new TestBean();
		model.put("tb", tb);

		HttpServletRequest request = new MockHttpServletRequest(sc);
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		view1.render(model, request, response);
		assertTrue("Correct tb attribute", tb.equals(request.getAttribute("tb")));
		assertTrue("Correct test1 attribute", "testvalue1".equals(request.getAttribute("test1")));
		assertTrue("Correct test2 attribute", testBean.equals(request.getAttribute("test2")));

		request = new MockHttpServletRequest(sc);
		response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		view2.render(model, request, response);
		assertTrue("Correct tb attribute", tb.equals(request.getAttribute("tb")));
		assertTrue("Correct test1 attribute", "testvalue1".equals(request.getAttribute("test1")));
		assertTrue("Correct test2 attribute", "testvalue2".equals(request.getAttribute("test2")));
	}

	@Test
	public void testXmlViewResolverDefaultLocation() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext() {
			@Override
			protected Resource getResourceByPath(String path) {
				assertTrue("Correct default location", XmlViewResolver.DEFAULT_LOCATION.equals(path));
				return super.getResourceByPath(path);
			}
		};
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		XmlViewResolver vr = new XmlViewResolver();
		try {
			vr.setApplicationContext(wac);
			vr.afterPropertiesSet();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
		}
	}

	@Test
	public void testXmlViewResolverWithoutCache() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext() {
			@Override
			protected Resource getResourceByPath(String path) {
				assertTrue("Correct default location", XmlViewResolver.DEFAULT_LOCATION.equals(path));
				return super.getResourceByPath(path);
			}
		};
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		XmlViewResolver vr = new XmlViewResolver();
		vr.setCache(false);
		try {
			vr.setApplicationContext(wac);
		}
		catch (ApplicationContextException ex) {
			fail("Should not have thrown ApplicationContextException: " + ex.getMessage());
		}
		try {
			vr.resolveViewName("example1", Locale.getDefault());
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
		}
	}

	@Test
	public void testCacheRemoval() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		vr.setViewClass(JstlView.class);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		View cached = vr.resolveViewName("example1", Locale.getDefault());
		if (view != cached) {
			fail("Caching doesn't work");
		}

		vr.removeFromCache("example1", Locale.getDefault());
		cached = vr.resolveViewName("example1", Locale.getDefault());
		if (view == cached) {
			// the chance of having the same reference (hashCode) twice if negligible).
			fail("View wasn't removed from cache");
		}
	}

	@Test
	public void testCacheUnresolved() throws Exception {
		final AtomicInteger count = new AtomicInteger();
		AbstractCachingViewResolver viewResolver = new AbstractCachingViewResolver() {
			@Override
			protected View loadView(String viewName, Locale locale) throws Exception {
				count.incrementAndGet();
				return null;
			}
		};

		viewResolver.setCacheUnresolved(false);

		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());

		assertEquals(2, count.intValue());

		viewResolver.setCacheUnresolved(true);

		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());

		assertEquals(3, count.intValue());
	}


	public static class TestView extends InternalResourceView {

		public void setLocation(Resource location) {
			if (!(location instanceof ServletContextResource)) {
				throw new IllegalArgumentException("Expecting ClassPathResource, not " + location.getClass().getName());
			}
		}
	}

}
