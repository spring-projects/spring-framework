package org.springframework.web.servlet.resource;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.resource.PathResourceResolver}.
 *
 * @author Brian Clozel
 */
public class PathResourceResolverTests {

	private ResourceResolverChain chain;

	private List<Resource> locations;

	@Before
	public void setup() {

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(new PathResourceResolver());
		this.chain = new DefaultResourceResolverChain(resolvers);

		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
	}

	@Test
	public void resolveResourceInternal() {
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		Resource actual = this.chain.resolveResource(null, file, this.locations);

		assertEquals(expected, actual);
	}

}
