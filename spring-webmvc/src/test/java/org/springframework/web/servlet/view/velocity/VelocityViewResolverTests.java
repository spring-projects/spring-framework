package org.springframework.web.servlet.view.velocity;

import java.util.Locale;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Test;

import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 */
public class VelocityViewResolverTests {

	@Test
	public void testVelocityViewResolver() throws Exception {
		VelocityConfig vc = new VelocityConfig() {
			@Override
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine("prefix_test_suffix", new Template());
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("configurer", vc);
		wac.refresh();

		VelocityViewResolver vr = new VelocityViewResolver();
		vr.setPrefix("prefix_");
		vr.setSuffix("_suffix");
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertEquals("Correct view class", VelocityView.class, view.getClass());
		assertEquals("Correct URL", "prefix_test_suffix", ((VelocityView) view).getUrl());

		view = vr.resolveViewName("non-existing", Locale.CANADA);
		assertNull(view);

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertEquals("Correct view class", RedirectView.class, view.getClass());
		assertEquals("Correct URL", "myUrl", ((RedirectView) view).getUrl());

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertEquals("Correct view class", InternalResourceView.class, view.getClass());
		assertEquals("Correct URL", "myUrl", ((InternalResourceView) view).getUrl());
	}

	@Test
	public void testVelocityViewResolverWithToolbox() throws Exception {
		VelocityConfig vc = new VelocityConfig() {
			@Override
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine("prefix_test_suffix", new Template());
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("configurer", vc);
		wac.refresh();

		String toolbox = "org/springframework/web/servlet/view/velocity/toolbox.xml";

		VelocityViewResolver vr = new VelocityViewResolver();
		vr.setPrefix("prefix_");
		vr.setSuffix("_suffix");
		vr.setToolboxConfigLocation(toolbox);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertEquals("Correct view class", VelocityToolboxView.class, view.getClass());
		assertEquals("Correct URL", "prefix_test_suffix", ((VelocityView) view).getUrl());
		assertEquals("Correct toolbox", toolbox, ((VelocityToolboxView) view).getToolboxConfigLocation());
	}

	@Test
	public void testVelocityViewResolverWithToolboxSubclass() throws Exception {
		VelocityConfig vc = new VelocityConfig() {
			@Override
			public VelocityEngine getVelocityEngine() {
				TestVelocityEngine ve = new TestVelocityEngine();
				ve.addTemplate("prefix_test_suffix", new Template());
				ve.addTemplate(VelocityLayoutView.DEFAULT_LAYOUT_URL, new Template());
				return ve;
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("configurer", vc);
		wac.refresh();

		String toolbox = "org/springframework/web/servlet/view/velocity/toolbox.xml";

		VelocityViewResolver vr = new VelocityViewResolver();
		vr.setViewClass(VelocityLayoutView.class);
		vr.setPrefix("prefix_");
		vr.setSuffix("_suffix");
		vr.setToolboxConfigLocation(toolbox);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertEquals("Correct view class", VelocityLayoutView.class, view.getClass());
		assertEquals("Correct URL", "prefix_test_suffix", ((VelocityView) view).getUrl());
		assertEquals("Correct toolbox", toolbox, ((VelocityToolboxView) view).getToolboxConfigLocation());
	}

	@Test
	public void testVelocityLayoutViewResolver() throws Exception {
		VelocityConfig vc = new VelocityConfig() {
			@Override
			public VelocityEngine getVelocityEngine() {
				TestVelocityEngine ve = new TestVelocityEngine();
				ve.addTemplate("prefix_test_suffix", new Template());
				ve.addTemplate("myLayoutUrl", new Template());
				return ve;
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("configurer", vc);
		wac.refresh();

		VelocityLayoutViewResolver vr = new VelocityLayoutViewResolver();
		vr.setPrefix("prefix_");
		vr.setSuffix("_suffix");
		vr.setLayoutUrl("myLayoutUrl");
		vr.setLayoutKey("myLayoutKey");
		vr.setScreenContentKey("myScreenContentKey");
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertEquals("Correct view class", VelocityLayoutView.class, view.getClass());
		assertEquals("Correct URL", "prefix_test_suffix", ((VelocityView) view).getUrl());
		// TODO: Need to test actual VelocityLayoutView properties and their functionality!
	}

}
