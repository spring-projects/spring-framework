package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;
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
	public void scalarList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("9");
		list.add("37");
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("scalarListTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(list, sourceType, targetType);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")		
		List<String> result = (List<String>) conversionService.convert(list, sourceType, targetType);
		assertFalse(list.equals(result));
		assertEquals((Integer) 9, result.get(0));
		assertEquals((Integer) 37, result.get(1));
	}
	
	public ArrayList<Integer> scalarListTarget;

	@Test
	public void emptyListToList() throws Exception {
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<String>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyListTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		assertEquals(list, conversionService.convert(list, sourceType, targetType));
	}

	public List<Integer> emptyListTarget;

	@Test
	public void emptyListToListDifferentTargetType() throws Exception {
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<String>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyListDifferentTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")
		LinkedList<Integer> result = (LinkedList<Integer>) conversionService.convert(list, sourceType, targetType);
		assertEquals(LinkedList.class, result.getClass());
		assertTrue(result.isEmpty());
	}

	public LinkedList<Integer> emptyListDifferentTarget;

	@Test
	public void collectionToObjectInteraction() throws Exception {
		List<List<String>> list = new ArrayList<List<String>>();
		list.add(Arrays.asList("9", "12"));
		list.add(Arrays.asList("37", "23"));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		assertTrue(conversionService.canConvert(List.class, List.class));
		assertEquals(list, conversionService.convert(list, List.class));
	}

	@Test
	public void arrayCollectionToObjectInteraction() throws Exception {
		List<String>[] array = new List[2];
		array[0] = Arrays.asList("9", "12");
		array[1] = Arrays.asList("37", "23");
		conversionService.addConverter(new ArrayToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		assertTrue(conversionService.canConvert(String[].class, List.class));
		assertEquals(Arrays.asList(array), conversionService.convert(array, List.class));
	}

	@Test
	public void objectToCollection() throws Exception {
		List<List<String>> list = new ArrayList<List<String>>();
		list.add(Arrays.asList("9", "12"));
		list.add(Arrays.asList("37", "23"));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		conversionService.addConverter(new ObjectToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("objectToCollection"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		List<List<List<Integer>>> result = (List<List<List<Integer>>>) conversionService.convert(list, sourceType, targetType);
		assertEquals((Integer)9, result.get(0).get(0).get(0));
		assertEquals((Integer)12, result.get(0).get(1).get(0));
		assertEquals((Integer)37, result.get(1).get(0).get(0));
		assertEquals((Integer)23, result.get(1).get(1).get(0));
	}

	public List<List<List<Integer>>> objectToCollection;
	
	@Test
	public void stringToCollection() throws Exception {
		List<List<String>> list = new ArrayList<List<String>>();
		list.add(Arrays.asList("9,12"));
		list.add(Arrays.asList("37,23"));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		conversionService.addConverter(new StringToCollectionConverter(conversionService));
		conversionService.addConverter(new ObjectToCollectionConverter(conversionService));		
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("objectToCollection"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		List<List<List<Integer>>> result = (List<List<List<Integer>>>) conversionService.convert(list, sourceType, targetType);
		assertEquals((Integer)9, result.get(0).get(0).get(0));
		assertEquals((Integer)12, result.get(0).get(0).get(1));
		assertEquals((Integer)37, result.get(1).get(0).get(0));
		assertEquals((Integer)23, result.get(1).get(0).get(1));
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

	@Test
	public void allNullsNotConvertible() throws Exception {
		List<Resource> resources = new ArrayList<Resource>();
		resources.add(null);
		resources.add(null);
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("allNullsNotConvertible")); 
		assertEquals(resources, conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources"))));
	}
	
	public List<String> allNullsNotConvertible;

	@Test(expected=ConversionFailedException.class)
	public void nothingInCommon() throws Exception {
		List<Object> resources = new ArrayList<Object>();
		resources.add(new ClassPathResource("test"));
		resources.add(3);
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertEquals(resources, conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources"))));
	}

	public List<Resource> resources;

	public static abstract class BaseResource implements Resource {

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

