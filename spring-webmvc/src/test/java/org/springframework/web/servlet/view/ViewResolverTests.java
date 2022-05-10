/*
 * Copyright 2002-2020 the original author or authors.
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

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.jstl.core.Config;
import jakarta.servlet.jsp.jstl.fmt.LocalizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockRequestDispatcher;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link BeanNameViewResolver}, {@link UrlBasedViewResolver},
 * {@link InternalResourceViewResolver}, {@link org.springframework.web.servlet.view.XmlViewResolver},
 * and {@link AbstractCachingViewResolver}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 18.06.2003
 */
public class ViewResolverTests {

	private final StaticWebApplicationContext wac = new StaticWebApplicationContext();
	private final MockServletContext sc = new MockServletContext();
	private final MockHttpServletRequest request = new MockHttpServletRequest(this.sc);
	private final HttpServletResponse response = new MockHttpServletResponse();

	@BeforeEach
	public void setUp() {
		this.wac.setServletContext(this.sc);
	}

	@Test
	public void beanNameViewResolver() {
		MutablePropertyValues pvs1 = new MutablePropertyValues();
		pvs1.addPropertyValue(new PropertyValue("url", "/example1.jsp"));
		this.wac.registerSingleton("example1", InternalResourceView.class, pvs1);
		MutablePropertyValues pvs2 = new MutablePropertyValues();
		pvs2.addPropertyValue(new PropertyValue("url", "/example2.jsp"));
		this.wac.registerSingleton("example2", JstlView.class, pvs2);
		BeanNameViewResolver vr = new BeanNameViewResolver();
		vr.setApplicationContext(this.wac);
		this.wac.refresh();

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/example1.jsp");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("/example2.jsp");
	}

	@Test
	public void urlBasedViewResolverOverridesCustomRequestContextAttributeWithNonNullValue() throws Exception {
		assertThat(new TestView().getRequestContextAttribute())
			.as("requestContextAttribute when instantiated directly")
			.isEqualTo("testRequestContext");

		UrlBasedViewResolver vr = new UrlBasedViewResolver();
		vr.setViewClass(TestView.class);
		vr.setApplicationContext(this.wac);
		vr.setRequestContextAttribute("viewResolverRequestContext");
		this.wac.refresh();

		View view = vr.resolveViewName("example", Locale.getDefault());
		assertThat(view).isInstanceOf(TestView.class);
		assertThat(((TestView) view).getRequestContextAttribute())
			.as("requestContextAttribute when instantiated dynamically by UrlBasedViewResolver")
			.isEqualTo("viewResolverRequestContext");
	}

	@Test
	public void urlBasedViewResolverDoesNotOverrideCustomRequestContextAttributeWithNull() throws Exception {
		assertThat(new TestView().getRequestContextAttribute())
			.as("requestContextAttribute when instantiated directly")
			.isEqualTo("testRequestContext");

		UrlBasedViewResolver vr = new UrlBasedViewResolver();
		vr.setViewClass(TestView.class);
		vr.setApplicationContext(this.wac);
		this.wac.refresh();

		View view = vr.resolveViewName("example", Locale.getDefault());
		assertThat(view).isInstanceOf(TestView.class);
		assertThat(((TestView) view).getRequestContextAttribute())
			.as("requestContextAttribute when instantiated dynamically by UrlBasedViewResolver")
			.isEqualTo("testRequestContext");
	}

	@Test
	public void urlBasedViewResolverWithoutPrefixes() throws Exception {
		UrlBasedViewResolver vr = new UrlBasedViewResolver();
		vr.setViewClass(JstlView.class);
		doTestUrlBasedViewResolverWithoutPrefixes(vr);
	}

	@Test
	public void urlBasedViewResolverWithPrefixes() throws Exception {
		UrlBasedViewResolver vr = new UrlBasedViewResolver();
		vr.setViewClass(JstlView.class);
		doTestUrlBasedViewResolverWithPrefixes(vr);
	}

	@Test
	public void internalResourceViewResolverWithoutPrefixes() throws Exception {
		doTestUrlBasedViewResolverWithoutPrefixes(new InternalResourceViewResolver());
	}

	@Test
	public void internalResourceViewResolverWithPrefixes() throws Exception {
		doTestUrlBasedViewResolverWithPrefixes(new InternalResourceViewResolver());
	}

	private void doTestUrlBasedViewResolverWithoutPrefixes(UrlBasedViewResolver vr) throws Exception {
		this.wac.refresh();
		vr.setApplicationContext(this.wac);
		vr.setContentType("myContentType");
		vr.setRequestContextAttribute("rc");

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example1");
		assertThat(((InternalResourceView) view).getContentType()).as("Correct textContentType").isEqualTo("myContentType");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example2");
		assertThat(((InternalResourceView) view).getContentType()).as("Correct textContentType").isEqualTo("myContentType");

		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		this.request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		this.request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, this.request, this.response);
		assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		boolean condition = this.request.getAttribute("rc") instanceof RequestContext;
		assertThat(condition).as("Correct rc attribute").isTrue();

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(RedirectView.class);
		assertThat(((RedirectView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
		assertThat(((RedirectView) view).getApplicationContext()).as("View not initialized as bean").isSameAs(this.wac);

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
	}

	private void doTestUrlBasedViewResolverWithPrefixes(UrlBasedViewResolver vr) throws Exception {
		this.wac.refresh();
		vr.setPrefix("/WEB-INF/");
		vr.setSuffix(".jsp");
		vr.setApplicationContext(this.wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/WEB-INF/example1.jsp");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/WEB-INF/example2.jsp");

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(RedirectView.class);
		assertThat(((RedirectView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
	}

	@Test
	public void internalResourceViewResolverWithAttributes() throws Exception {
		this.wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		Properties props = new Properties();
		props.setProperty("key1", "value1");
		vr.setAttributes(props);
		Map<String, Object> map = new HashMap<>();
		map.put("key2", 2);
		vr.setAttributesMap(map);
		vr.setApplicationContext(this.wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example1");
		Map<String, Object> attributes = ((InternalResourceView) view).getStaticAttributes();
		assertThat(attributes.get("key1")).isEqualTo("value1");
		assertThat(attributes.get("key2")).isEqualTo(2);

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example2");
		attributes = ((InternalResourceView) view).getStaticAttributes();
		assertThat(attributes.get("key1")).isEqualTo("value1");
		assertThat(attributes.get("key2")).isEqualTo(2);

		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		this.request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, this.request, this.response);

		assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat(this.request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
		assertThat(this.request.getAttribute("key1")).isEqualTo("value1");
		assertThat(this.request.getAttribute("key2")).isEqualTo(2);
	}

	@Test
	public void internalResourceViewResolverWithContextBeans() throws Exception {
		this.wac.registerSingleton("myBean", TestBean.class);
		this.wac.registerSingleton("myBean2", TestBean.class);
		this.wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		Properties props = new Properties();
		props.setProperty("key1", "value1");
		vr.setAttributes(props);
		Map<String, Object> map = new HashMap<>();
		map.put("key2", 2);
		vr.setAttributesMap(map);
		vr.setExposeContextBeansAsAttributes(true);
		vr.setApplicationContext(this.wac);

		HttpServletRequest request = new MockHttpServletRequest(this.sc) {
			@Override
			public RequestDispatcher getRequestDispatcher(String path) {
				return new MockRequestDispatcher(path) {
					@Override
					public void forward(ServletRequest forwardRequest, ServletResponse forwardResponse) {
						assertThat(forwardRequest.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
						assertThat(forwardRequest.getAttribute("key1")).isEqualTo("value1");
						assertThat(forwardRequest.getAttribute("key2")).isEqualTo(2);
						assertThat(forwardRequest.getAttribute("myBean")).isSameAs(wac.getBean("myBean"));
						assertThat(forwardRequest.getAttribute("myBean2")).isSameAs(wac.getBean("myBean2"));
					}
				};
			}
		};
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		View view = vr.resolveViewName("example1", Locale.getDefault());
		view.render(new HashMap<String, Object>(), request, this.response);
	}

	@Test
	public void internalResourceViewResolverWithSpecificContextBeans() throws Exception {
		this.wac.registerSingleton("myBean", TestBean.class);
		this.wac.registerSingleton("myBean2", TestBean.class);
		this.wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		Properties props = new Properties();
		props.setProperty("key1", "value1");
		vr.setAttributes(props);
		Map<String, Object> map = new HashMap<>();
		map.put("key2", 2);
		vr.setAttributesMap(map);
		vr.setExposedContextBeanNames(new String[] {"myBean2"});
		vr.setApplicationContext(this.wac);

		HttpServletRequest request = new MockHttpServletRequest(this.sc) {
			@Override
			public RequestDispatcher getRequestDispatcher(String path) {
				return new MockRequestDispatcher(path) {
					@Override
					public void forward(ServletRequest forwardRequest, ServletResponse forwardResponse) {
						assertThat(forwardRequest.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
						assertThat(forwardRequest.getAttribute("key1")).isEqualTo("value1");
						assertThat(forwardRequest.getAttribute("key2")).isEqualTo(2);
						assertThat(forwardRequest.getAttribute("myBean")).isNull();
						assertThat(forwardRequest.getAttribute("myBean2")).isSameAs(wac.getBean("myBean2"));
					}
				};
			}
		};
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		View view = vr.resolveViewName("example1", Locale.getDefault());
		view.render(new HashMap<String, Object>(), request, this.response);
	}

	@Test
	public void internalResourceViewResolverWithJstl() throws Exception {
		Locale locale = !Locale.GERMAN.equals(Locale.getDefault()) ? Locale.GERMAN : Locale.FRENCH;

		this.wac.addMessage("code1", locale, "messageX");
		this.wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		vr.setViewClass(JstlView.class);
		vr.setApplicationContext(this.wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example1");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example2");

		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		this.request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(locale));
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, this.request, this.response);

		assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat(this.request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();

		assertThat(Config.get(this.request, Config.FMT_LOCALE)).isEqualTo(locale);
		LocalizationContext lc = (LocalizationContext) Config.get(this.request, Config.FMT_LOCALIZATION_CONTEXT);
		assertThat(lc.getResourceBundle().getString("code1")).isEqualTo("messageX");
	}

	@Test
	public void internalResourceViewResolverWithJstlAndContextParam() throws Exception {
		Locale locale = !Locale.GERMAN.equals(Locale.getDefault()) ? Locale.GERMAN : Locale.FRENCH;

		this.sc.addInitParameter(Config.FMT_LOCALIZATION_CONTEXT, "org/springframework/web/context/WEB-INF/context-messages");
		this.wac.addMessage("code1", locale, "messageX");
		this.wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		vr.setViewClass(JstlView.class);
		vr.setApplicationContext(this.wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example1");

		view = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(view).isInstanceOf(JstlView.class);
		assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example2");

		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		this.request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(locale));
		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);
		view.render(model, this.request, this.response);

		assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat(this.request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();

		assertThat(Config.get(this.request, Config.FMT_LOCALE)).isEqualTo(locale);
		LocalizationContext lc = (LocalizationContext) Config.get(this.request, Config.FMT_LOCALIZATION_CONTEXT);
		assertThat(lc.getResourceBundle().getString("code1")).isEqualTo("message1");
		assertThat(lc.getResourceBundle().getString("code2")).isEqualTo("message2");
	}

	@Test
	@SuppressWarnings("deprecation")
	public void xmlViewResolver() throws Exception {
		this.wac.registerSingleton("testBean", TestBean.class);
		this.wac.refresh();
		TestBean testBean = (TestBean) this.wac.getBean("testBean");
		org.springframework.web.servlet.view.XmlViewResolver vr = new org.springframework.web.servlet.view.XmlViewResolver();
		vr.setLocation(new ClassPathResource("org/springframework/web/servlet/view/views.xml"));
		vr.setApplicationContext(this.wac);

		View view1 = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(TestView.class.equals(view1.getClass())).as("Correct view class").isTrue();
		assertThat("/example1.jsp".equals(((InternalResourceView) view1).getUrl())).as("Correct URL").isTrue();

		View view2 = vr.resolveViewName("example2", Locale.getDefault());
		assertThat(JstlView.class.equals(view2.getClass())).as("Correct view class").isTrue();
		assertThat("/example2new.jsp".equals(((InternalResourceView) view2).getUrl())).as("Correct URL").isTrue();

		Map<String, Object> model = new HashMap<>();
		TestBean tb = new TestBean();
		model.put("tb", tb);

		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		this.request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		this.request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		view1.render(model, this.request, this.response);
		assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat("testvalue1".equals(this.request.getAttribute("test1"))).as("Correct test1 attribute").isTrue();
		assertThat(testBean.equals(this.request.getAttribute("test2"))).as("Correct test2 attribute").isTrue();

		this.request.clearAttributes();
		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		this.request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		this.request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		view2.render(model, this.request, this.response);
		assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
		assertThat("testvalue1".equals(this.request.getAttribute("test1"))).as("Correct test1 attribute").isTrue();
		assertThat("testvalue2".equals(this.request.getAttribute("test2"))).as("Correct test2 attribute").isTrue();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void xmlViewResolverDefaultLocation() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext() {
			@Override
			protected Resource getResourceByPath(String path) {
				assertThat(org.springframework.web.servlet.view.XmlViewResolver.DEFAULT_LOCATION.equals(path)).as("Correct default location").isTrue();
				return super.getResourceByPath(path);
			}
		};
		wac.setServletContext(this.sc);
		wac.refresh();
		org.springframework.web.servlet.view.XmlViewResolver vr = new org.springframework.web.servlet.view.XmlViewResolver();
		vr.setApplicationContext(wac);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(vr::afterPropertiesSet);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void xmlViewResolverWithoutCache() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext() {
			@Override
			protected Resource getResourceByPath(String path) {
				assertThat(org.springframework.web.servlet.view.XmlViewResolver.DEFAULT_LOCATION.equals(path)).as("Correct default location").isTrue();
				return super.getResourceByPath(path);
			}
		};
		wac.setServletContext(this.sc);
		wac.refresh();
		org.springframework.web.servlet.view.XmlViewResolver vr = new org.springframework.web.servlet.view.XmlViewResolver();
		vr.setCache(false);
		vr.setApplicationContext(wac);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				vr.resolveViewName("example1", Locale.getDefault()));
	}

	@Test
	public void cacheRemoval() throws Exception {
		this.wac.refresh();
		InternalResourceViewResolver vr = new InternalResourceViewResolver();
		vr.setViewClass(JstlView.class);
		vr.setApplicationContext(this.wac);

		View view = vr.resolveViewName("example1", Locale.getDefault());
		View cached = vr.resolveViewName("example1", Locale.getDefault());
		assertThat(cached).isSameAs(view);

		vr.removeFromCache("example1", Locale.getDefault());
		cached = vr.resolveViewName("example1", Locale.getDefault());
		// the chance of having the same reference (hashCode) twice is negligible.
		assertThat(cached).as("removed from cache").isNotSameAs(view);
	}

	@Test
	public void cacheUnresolved() throws Exception {
		final AtomicInteger count = new AtomicInteger();
		AbstractCachingViewResolver viewResolver = new AbstractCachingViewResolver() {
			@Override
			protected View loadView(String viewName, Locale locale) {
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

	@Test
	public void cacheFilterEnabled() throws Exception {
		AtomicInteger count = new AtomicInteger();

		// filter is enabled by default
		AbstractCachingViewResolver viewResolver = new AbstractCachingViewResolver() {
			@Override
			protected View loadView(String viewName, Locale locale) {
				assertThat(viewName).isEqualTo("view");
				assertThat(locale).isEqualTo(Locale.getDefault());
				count.incrementAndGet();
				return new TestView();
			}
		};

		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());

		assertThat(count.intValue()).isEqualTo(1);
	}

	@Test
	public void cacheFilterDisabled() throws Exception {
		AtomicInteger count = new AtomicInteger();

		AbstractCachingViewResolver viewResolver = new AbstractCachingViewResolver() {
			@Override
			protected View loadView(String viewName, Locale locale) {
				count.incrementAndGet();
				return new TestView();
			}
		};

		viewResolver.setCacheFilter((view, viewName, locale) -> false);

		viewResolver.resolveViewName("view", Locale.getDefault());
		viewResolver.resolveViewName("view", Locale.getDefault());

		assertThat(count.intValue()).isEqualTo(2);
	}


	public static class TestView extends InternalResourceView {

		public TestView() {
			setRequestContextAttribute("testRequestContext");
		}

		public void setLocation(Resource location) {
			if (!(location instanceof ServletContextResource)) {
				throw new IllegalArgumentException("Expecting ServletContextResource, not " + location.getClass().getName());
			}
		}
	}

}
