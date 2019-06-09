/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
		assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/example1.jsp");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("/example2.jsp");
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
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example1");
		assertThat(((InternalResourceView) view).getContentType()).as("Correct textContentType").isEqualTo("myContentType");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example2");
		assertThat(((InternalResourceView) view).getContentType()).as("Correct textContentType").isEqualTo("myContentType");

		HttpServletRequest request = new MockHttpServletRequest(wac.getServletContext());
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, request, response);
		assertThat(tb.equals(request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		boolean condition = request.getAttribute("rc") instanceof RequestContext;
		assertThat(condition).as("Correct rc attribute").isTrue();

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(RedirectView.class);
		assertThat(((RedirectView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
		assertThat(((RedirectView) view).getApplicationContext()).as("View not initialized as bean").isSameAs(wac);

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
	}

	private void doTestUrlBasedViewResolverWithPrefixes(UrlBasedViewResolver vr) throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		vr.setPrefix("/WEB-INF/");
		vr.setSuffix(".jsp");
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/WEB-INF/example1.jsp");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/WEB-INF/example2.jsp");

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(RedirectView.class);
		assertThat(((RedirectView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
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
		Map<String, Object> map = new HashMap<>();
		map.put("key2", new Integer(2));
		vr.setAttributesMap(map);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example1");
		Map<String, Object> attributes = ((InternalResourceView) view).getStaticAttributes();
		assertThat(attributes.get("key1")).isEqualTo("value1");
		assertThat(attributes.get("key2")).isEqualTo(new Integer(2));

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example2");
		attributes = ((InternalResourceView) view).getStaticAttributes();
		assertThat(attributes.get("key1")).isEqualTo("value1");
		assertThat(attributes.get("key2")).isEqualTo(new Integer(2));

		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, request, response);

		assertThat(tb.equals(request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat(request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
		assertThat(request.getAttribute("key1")).isEqualTo("value1");
		assertThat(request.getAttribute("key2")).isEqualTo(new Integer(2));
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
		Map<String, Object> map = new HashMap<>();
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
						assertThat(forwardRequest.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
						assertThat(forwardRequest.getAttribute("key1")).isEqualTo("value1");
						assertThat(forwardRequest.getAttribute("key2")).isEqualTo(new Integer(2));
						assertThat(forwardRequest.getAttribute("myBean")).isSameAs(wac.getBean("myBean"));
						assertThat(forwardRequest.getAttribute("myBean2")).isSameAs(wac.getBean("myBean2"));
					}
				};
			}
		};
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		View view = vr.resolveViewName("example1", Locale.getDefault());
		view.render(new HashMap<String, Object>(), request, response);
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
		Map<String, Object> map = new HashMap<>();
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
						assertThat(forwardRequest.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
						assertThat(forwardRequest.getAttribute("key1")).isEqualTo("value1");
						assertThat(forwardRequest.getAttribute("key2")).isEqualTo(new Integer(2));
						assertThat(forwardRequest.getAttribute("myBean")).isNull();
						assertThat(forwardRequest.getAttribute("myBean2")).isSameAs(wac.getBean("myBean2"));
					}
				};
			}
		};
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		View view = vr.resolveViewName("example1", Locale.getDefault());
		view.render(new HashMap<String, Object>(), request, response);
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
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example1");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example2");

		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(locale));
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, request, response);

		assertThat(tb.equals(request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat(request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();

		assertThat(Config.get(request, Config.FMT_LOCALE)).isEqualTo(locale);
		LocalizationContext lc = (LocalizationContext) Config.get(request, Config.FMT_LOCALIZATION_CONTEXT);
		assertThat(lc.getResourceBundle().getString("code1")).isEqualTo("messageX");
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
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example1");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example2");

		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(locale));
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, request, response);

		assertThat(tb.equals(request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat(request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();

		assertThat(Config.get(request, Config.FMT_LOCALE)).isEqualTo(locale);
		LocalizationContext lc = (LocalizationContext) Config.get(request, Config.FMT_LOCALIZATION_CONTEXT);
		assertThat(lc.getResourceBundle().getString("code1")).isEqualTo("message1");
		assertThat(lc.getResourceBundle().getString("code2")).isEqualTo("message2");
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
		assertThat(TestView.class.equals(view1.getClass())).as("Correct view class").isTrue();
		assertThat("/example1.jsp".equals(((InternalResourceView) view1).getUrl())).as("Correct URL").isTrue();

		View view2 = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(JstlView.class.equals(view2.getClass())).as("Correct view class").isTrue();
		assertThat("/example2new.jsp".equals(((InternalResourceView) view2).getUrl())).as("Correct URL").isTrue();

		ServletContext sc = new MockServletContext();
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);

		HttpServletRequest request = new MockHttpServletRequest(sc);
		HttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		view1.render(model, request, response);
		assertThat(tb.equals(request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat("testvalue1".equals(request.getAttribute("test1"))).as("Correct test1 attribute").isTrue();
		assertThat(testBean.equals(request.getAttribute("test2"))).as("Correct test2 attribute").isTrue();

		request = new MockHttpServletRequest(sc);
		response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		view2.render(model, request, response);
		assertThat(tb.equals(request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat("testvalue1".equals(request.getAttribute("test1"))).as("Correct test1 attribute").isTrue();
		assertThat("testvalue2".equals(request.getAttribute("test2"))).as("Correct test2 attribute").isTrue();
	}

	@Test
	public void testXmlViewResolverDefaultLocation() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext() {
			@Override
			protected Resource getResourceByPath(String path) {
				assertThat(XmlViewResolver.DEFAULT_LOCATION.equals(path)).as("Correct default location").isTrue();
				return super.getResourceByPath(path);
			}
		};
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		XmlViewResolver vr = new XmlViewResolver();
		vr.setApplicationContext(wac);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(
				vr::afterPropertiesSet);
	}

	@Test
	public void testXmlViewResolverWithoutCache() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext() {
			@Override
			protected Resource getResourceByPath(String path) {
				assertThat(XmlViewResolver.DEFAULT_LOCATION.equals(path)).as("Correct default location").isTrue();
				return super.getResourceByPath(path);
			}
		};
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		XmlViewResolver vr = new XmlViewResolver();
		vr.setCache(false);
		vr.setApplicationContext(wac);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				vr.resolveViewName("example1", Locale.getDefault()));
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
		assertThat(cached).isSameAs(view);

		vr.removeFromCache("example1", Locale.getDefault());
		cached = vr.resolveViewName("example1", Locale.getDefault());
		// the chance of having the same reference (hashCode) twice is negligible.
		assertThat(cached).as("removed from cache").isNotSameAs(view);
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

		assertThat(count.intValue()).isEqualTo(2);

		viewResolver.setCacheUnresolved(true);

		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());

		assertThat(count.intValue()).isEqualTo(3);
	}


	public static class TestView extends InternalResourceView {

		public void setLocation(Resource location) {
			if (!(location instanceof ServletContextResource)) {
				throw new IllegalArgumentException("Expecting ClassPathResource, not " + location.getClass().getName());
			}
		}
	}

}
