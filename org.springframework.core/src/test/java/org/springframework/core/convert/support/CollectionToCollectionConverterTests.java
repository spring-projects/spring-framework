package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class CollectionToCollectionConverterTests {

	private GenericConversionService conversionService = new GenericConversionService();

	@Before
	public void setUp() {
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
	}

	@Test
	public void differentImpls() throws Exception {
		List<Resource> resources = new ArrayList<Resource>();
		resources.add(new ClassPathResource("test"));
		resources.add(new FileSystemResource("test"));
		resources.add(new TestResource());
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertEquals(resources, conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources"))));
	}

	@Test
	public void mixedInNulls() throws Exception {
		List<Resource> resources = new ArrayList<Resource>();
		resources.add(new ClassPathResource("test"));
		resources.add(null);
		resources.add(new FileSystemResource("test"));
		resources.add(new TestResource());
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertEquals(resources, conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources"))));
	}

	@Test
	public void allNulls() throws Exception {
		List<Resource> resources = new ArrayList<Resource>();
		resources.add(null);
		resources.add(null);
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertEquals(resources, conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources"))));
	}
	
	@Test(expected=ConverterNotFoundException.class)
	public void nothingInCommon() throws Exception {
		List<Object> resources = new ArrayList<Object>();
		resources.add(new ClassPathResource("test"));
		resources.add(3);
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertEquals(resources, conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources"))));
	}

	public List<Resource> resources;

	public static  abstract class BaseResource implements Resource {

		public InputStream getInputStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean exists() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isReadable() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isOpen() {
			// TODO Auto-generated method stub
			return false;
		}

		public URL getURL() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public URI getURI() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public File getFile() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public long contentLength() throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}

		public long lastModified() throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}

		public Resource createRelative(String relativePath) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getFilename() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	public static class TestResource extends BaseResource {
		
	}
}

