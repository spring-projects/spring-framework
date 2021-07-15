/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.multipart.commons;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.testfixture.servlet.MockFilterConfig;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.testfixture.servlet.PassThroughFilterChain;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 08.10.2003
 */
public class CommonsMultipartResolverTests {

	@Test
	public void isMultipartWithDefaultSetting() {
		CommonsMultipartResolver resolver = new CommonsMultipartResolver();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_MIXED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_RELATED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request = new MockHttpServletRequest("PUT", "/");
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_MIXED_VALUE);
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_RELATED_VALUE);
		assertThat(resolver.isMultipart(request)).isFalse();
	}

	@Test
	public void isMultipartWithSupportedMethods() {
		CommonsMultipartResolver resolver = new CommonsMultipartResolver();
		resolver.setSupportedMethods("POST", "PUT");

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_MIXED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_RELATED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request = new MockHttpServletRequest("PUT", "/");
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_MIXED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_RELATED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();
	}

	@Test
	public void withApplicationContext() throws Exception {
		doTestWithApplicationContext(false);
	}

	@Test
	public void withApplicationContextAndLazyResolution() throws Exception {
		doTestWithApplicationContext(true);
	}

	private void doTestWithApplicationContext(boolean lazy) throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.getServletContext().setAttribute(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File("mytemp"));
		wac.refresh();
		MockCommonsMultipartResolver resolver = new MockCommonsMultipartResolver();
		resolver.setMaxUploadSize(1000);
		resolver.setMaxInMemorySize(100);
		resolver.setDefaultEncoding("enc");
		if (lazy) {
			resolver.setResolveLazily(false);
		}
		resolver.setServletContext(wac.getServletContext());
		assertThat(resolver.getFileUpload().getSizeMax()).isEqualTo(1000);
		assertThat(resolver.getFileItemFactory().getSizeThreshold()).isEqualTo(100);
		assertThat(resolver.getFileUpload().getHeaderEncoding()).isEqualTo("enc");
		assertThat(resolver.getFileItemFactory().getRepository().getAbsolutePath().endsWith("mytemp")).isTrue();

		MockHttpServletRequest originalRequest = new MockHttpServletRequest();
		originalRequest.setMethod("POST");
		originalRequest.setContentType("multipart/form-data");
		originalRequest.addHeader("Content-type", "multipart/form-data");
		originalRequest.addParameter("getField", "getValue");
		assertThat(resolver.isMultipart(originalRequest)).isTrue();
		MultipartHttpServletRequest request = resolver.resolveMultipart(originalRequest);

		doTestParameters(request);

		doTestFiles(request);

		doTestBinding(resolver, originalRequest, request);

		wac.close();
	}

	private void doTestParameters(MultipartHttpServletRequest request) {
		Set<String> parameterNames = new HashSet<>();
		Enumeration<String> parameterEnum = request.getParameterNames();
		while (parameterEnum.hasMoreElements()) {
			parameterNames.add(parameterEnum.nextElement());
		}
		assertThat(parameterNames.size()).isEqualTo(3);
		assertThat(parameterNames.contains("field3")).isTrue();
		assertThat(parameterNames.contains("field4")).isTrue();
		assertThat(parameterNames.contains("getField")).isTrue();
		assertThat(request.getParameter("field3")).isEqualTo("value3");
		List<String> parameterValues = Arrays.asList(request.getParameterValues("field3"));
		assertThat(parameterValues.size()).isEqualTo(1);
		assertThat(parameterValues.contains("value3")).isTrue();
		assertThat(request.getParameter("field4")).isEqualTo("value4");
		parameterValues = Arrays.asList(request.getParameterValues("field4"));
		assertThat(parameterValues.size()).isEqualTo(2);
		assertThat(parameterValues.contains("value4")).isTrue();
		assertThat(parameterValues.contains("value5")).isTrue();
		assertThat(request.getParameter("field4")).isEqualTo("value4");
		assertThat(request.getParameter("getField")).isEqualTo("getValue");

		List<String> parameterMapKeys = new ArrayList<>();
		List<Object> parameterMapValues = new ArrayList<>();
		for (Object o : request.getParameterMap().keySet()) {
			String key = (String) o;
			parameterMapKeys.add(key);
			parameterMapValues.add(request.getParameterMap().get(key));
		}
		assertThat(parameterMapKeys.size()).isEqualTo(3);
		assertThat(parameterMapValues.size()).isEqualTo(3);
		int field3Index = parameterMapKeys.indexOf("field3");
		int field4Index = parameterMapKeys.indexOf("field4");
		int getFieldIndex = parameterMapKeys.indexOf("getField");
		assertThat(field3Index != -1).isTrue();
		assertThat(field4Index != -1).isTrue();
		assertThat(getFieldIndex != -1).isTrue();
		parameterValues = Arrays.asList((String[]) parameterMapValues.get(field3Index));
		assertThat(parameterValues.size()).isEqualTo(1);
		assertThat(parameterValues.contains("value3")).isTrue();
		parameterValues = Arrays.asList((String[]) parameterMapValues.get(field4Index));
		assertThat(parameterValues.size()).isEqualTo(2);
		assertThat(parameterValues.contains("value4")).isTrue();
		assertThat(parameterValues.contains("value5")).isTrue();
		parameterValues = Arrays.asList((String[]) parameterMapValues.get(getFieldIndex));
		assertThat(parameterValues.size()).isEqualTo(1);
		assertThat(parameterValues.contains("getValue")).isTrue();
	}

	private void doTestFiles(MultipartHttpServletRequest request) throws IOException {
		Set<String> fileNames = new HashSet<>();
		Iterator<String> fileIter = request.getFileNames();
		while (fileIter.hasNext()) {
			fileNames.add(fileIter.next());
		}
		assertThat(fileNames.size()).isEqualTo(3);
		assertThat(fileNames.contains("field1")).isTrue();
		assertThat(fileNames.contains("field2")).isTrue();
		assertThat(fileNames.contains("field2x")).isTrue();
		CommonsMultipartFile file1 = (CommonsMultipartFile) request.getFile("field1");
		CommonsMultipartFile file2 = (CommonsMultipartFile) request.getFile("field2");
		CommonsMultipartFile file2x = (CommonsMultipartFile) request.getFile("field2x");

		Map<String, MultipartFile> fileMap = request.getFileMap();
		assertThat(fileMap.size()).isEqualTo(3);
		assertThat(fileMap.containsKey("field1")).isTrue();
		assertThat(fileMap.containsKey("field2")).isTrue();
		assertThat(fileMap.containsKey("field2x")).isTrue();
		assertThat(fileMap.get("field1")).isEqualTo(file1);
		assertThat(fileMap.get("field2")).isEqualTo(file2);
		assertThat(fileMap.get("field2x")).isEqualTo(file2x);

		MultiValueMap<String, MultipartFile> multiFileMap = request.getMultiFileMap();
		assertThat(multiFileMap.size()).isEqualTo(3);
		assertThat(multiFileMap.containsKey("field1")).isTrue();
		assertThat(multiFileMap.containsKey("field2")).isTrue();
		assertThat(multiFileMap.containsKey("field2x")).isTrue();
		List<MultipartFile> field1Files = multiFileMap.get("field1");
		assertThat(field1Files.size()).isEqualTo(2);
		assertThat(field1Files.contains(file1)).isTrue();
		assertThat(multiFileMap.getFirst("field1")).isEqualTo(file1);
		assertThat(multiFileMap.getFirst("field2")).isEqualTo(file2);
		assertThat(multiFileMap.getFirst("field2x")).isEqualTo(file2x);

		assertThat(file1.getContentType()).isEqualTo("type1");
		assertThat(file2.getContentType()).isEqualTo("type2");
		assertThat(file2x.getContentType()).isEqualTo("type2");
		assertThat(file1.getOriginalFilename()).isEqualTo("field1.txt");
		assertThat(file2.getOriginalFilename()).isEqualTo("field2.txt");
		assertThat(file2x.getOriginalFilename()).isEqualTo("field2x.txt");
		assertThat(new String(file1.getBytes())).isEqualTo("text1");
		assertThat(new String(file2.getBytes())).isEqualTo("text2");
		assertThat(file1.getSize()).isEqualTo(5);
		assertThat(file2.getSize()).isEqualTo(5);
		boolean condition1 = file1.getInputStream() instanceof ByteArrayInputStream;
		assertThat(condition1).isTrue();
		boolean condition = file2.getInputStream() instanceof ByteArrayInputStream;
		assertThat(condition).isTrue();
		File transfer1 = new File("C:/transfer1");
		file1.transferTo(transfer1);
		File transfer2 = new File("C:/transfer2");
		file2.transferTo(transfer2);
		assertThat(((MockFileItem) file1.getFileItem()).writtenFile).isEqualTo(transfer1);
		assertThat(((MockFileItem) file2.getFileItem()).writtenFile).isEqualTo(transfer2);

	}

	private void doTestBinding(MockCommonsMultipartResolver resolver, MockHttpServletRequest originalRequest,
			MultipartHttpServletRequest request) throws UnsupportedEncodingException {

		MultipartTestBean1 mtb1 = new MultipartTestBean1();
		assertThat(mtb1.getField1()).isEqualTo(null);
		assertThat(mtb1.getField2()).isEqualTo(null);
		ServletRequestDataBinder binder = new ServletRequestDataBinder(mtb1, "mybean");
		binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
		binder.bind(request);
		List<MultipartFile> file1List = request.getFiles("field1");
		CommonsMultipartFile file1a = (CommonsMultipartFile) file1List.get(0);
		CommonsMultipartFile file1b = (CommonsMultipartFile) file1List.get(1);
		CommonsMultipartFile file2 = (CommonsMultipartFile) request.getFile("field2");
		assertThat(mtb1.getField1()[0]).isEqualTo(file1a);
		assertThat(mtb1.getField1()[1]).isEqualTo(file1b);
		assertThat(new String(mtb1.getField2())).isEqualTo(new String(file2.getBytes()));

		MultipartTestBean2 mtb2 = new MultipartTestBean2();
		assertThat(mtb2.getField1()).isEqualTo(null);
		assertThat(mtb2.getField2()).isEqualTo(null);
		binder = new ServletRequestDataBinder(mtb2, "mybean");
		binder.registerCustomEditor(String.class, "field1", new StringMultipartFileEditor());
		binder.registerCustomEditor(String.class, "field2", new StringMultipartFileEditor("UTF-16"));
		binder.bind(request);
		assertThat(mtb2.getField1()[0]).isEqualTo(new String(file1a.getBytes()));
		assertThat(mtb2.getField1()[1]).isEqualTo(new String(file1b.getBytes()));
		assertThat(mtb2.getField2()).isEqualTo(new String(file2.getBytes(), "UTF-16"));

		resolver.cleanupMultipart(request);
		assertThat(((MockFileItem) file1a.getFileItem()).deleted).isTrue();
		assertThat(((MockFileItem) file1b.getFileItem()).deleted).isTrue();
		assertThat(((MockFileItem) file2.getFileItem()).deleted).isTrue();

		resolver.setEmpty(true);
		request = resolver.resolveMultipart(originalRequest);
		binder.setBindEmptyMultipartFiles(false);
		String firstBound = mtb2.getField2();
		binder.bind(request);
		assertThat(mtb2.getField2().isEmpty()).isFalse();
		assertThat(mtb2.getField2()).isEqualTo(firstBound);

		request = resolver.resolveMultipart(originalRequest);
		binder.setBindEmptyMultipartFiles(true);
		binder.bind(request);
		assertThat(mtb2.getField2().isEmpty()).isTrue();
	}

	@Test
	public void withServletContextAndFilter() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.registerSingleton("filterMultipartResolver", MockCommonsMultipartResolver.class, new MutablePropertyValues());
		wac.getServletContext().setAttribute(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File("mytemp"));
		wac.refresh();
		wac.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		CommonsMultipartResolver resolver = new CommonsMultipartResolver(wac.getServletContext());
		assertThat(resolver.getFileItemFactory().getRepository().getAbsolutePath().endsWith("mytemp")).isTrue();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		filterConfig.addInitParameter("class", "notWritable");
		filterConfig.addInitParameter("unknownParam", "someValue");
		final MultipartFilter filter = new MultipartFilter();
		filter.init(filterConfig);

		final List<MultipartFile> files = new ArrayList<>();
		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				MultipartHttpServletRequest request = (MultipartHttpServletRequest) servletRequest;
				files.addAll(request.getFileMap().values());
			}
		};

		FilterChain filterChain2 = new PassThroughFilterChain(filter, filterChain);

		MockHttpServletRequest originalRequest = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		originalRequest.setMethod("POST");
		originalRequest.setContentType("multipart/form-data");
		originalRequest.addHeader("Content-type", "multipart/form-data");
		filter.doFilter(originalRequest, response, filterChain2);

		CommonsMultipartFile file1 = (CommonsMultipartFile) files.get(0);
		CommonsMultipartFile file2 = (CommonsMultipartFile) files.get(1);
		assertThat(((MockFileItem) file1.getFileItem()).deleted).isTrue();
		assertThat(((MockFileItem) file2.getFileItem()).deleted).isTrue();
	}

	@Test
	public void withServletContextAndFilterWithCustomBeanName() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		wac.registerSingleton("myMultipartResolver", MockCommonsMultipartResolver.class, new MutablePropertyValues());
		wac.getServletContext().setAttribute(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File("mytemp"));
		wac.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		CommonsMultipartResolver resolver = new CommonsMultipartResolver(wac.getServletContext());
		assertThat(resolver.getFileItemFactory().getRepository().getAbsolutePath().endsWith("mytemp")).isTrue();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		filterConfig.addInitParameter("multipartResolverBeanName", "myMultipartResolver");

		final List<MultipartFile> files = new ArrayList<>();
		FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest originalRequest, ServletResponse response) {
				if (originalRequest instanceof MultipartHttpServletRequest) {
					MultipartHttpServletRequest request = (MultipartHttpServletRequest) originalRequest;
					files.addAll(request.getFileMap().values());
				}
			}
		};

		MultipartFilter filter = new MultipartFilter() {
			private boolean invoked = false;
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
					FilterChain filterChain) throws ServletException, IOException {
				super.doFilterInternal(request, response, filterChain);
				super.doFilterInternal(request, response, filterChain);
				if (invoked) {
					throw new ServletException("Should not have been invoked twice");
				}
				invoked = true;
			}
		};
		filter.init(filterConfig);

		MockHttpServletRequest originalRequest = new MockHttpServletRequest();
		originalRequest.setMethod("POST");
		originalRequest.setContentType("multipart/form-data");
		originalRequest.addHeader("Content-type", "multipart/form-data");
		HttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(originalRequest, response, filterChain);
		CommonsMultipartFile file1 = (CommonsMultipartFile) files.get(0);
		CommonsMultipartFile file2 = (CommonsMultipartFile) files.get(1);
		assertThat(((MockFileItem) file1.getFileItem()).deleted).isTrue();
		assertThat(((MockFileItem) file2.getFileItem()).deleted).isTrue();
	}


	public static class MockCommonsMultipartResolver extends CommonsMultipartResolver {

		private boolean empty;

		protected void setEmpty(boolean empty) {
			this.empty = empty;
		}

		@Override
		protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
			return new ServletFileUpload() {
				@Override
				public List<FileItem> parseRequest(HttpServletRequest request) {
					if (request instanceof MultipartHttpServletRequest) {
						throw new IllegalStateException("Already a multipart request");
					}
					List<FileItem> fileItems = new ArrayList<>();
					MockFileItem fileItem1 = new MockFileItem(
						"field1", "type1", empty ? "" : "field1.txt", empty ? "" : "text1");
					MockFileItem fileItem1x = new MockFileItem(
						"field1", "type1", empty ? "" : "field1.txt", empty ? "" : "text1");
					MockFileItem fileItem2 = new MockFileItem(
						"field2", "type2", empty ? "" : "C:\\mypath/field2.txt", empty ? "" : "text2");
					MockFileItem fileItem2x = new MockFileItem(
						"field2x", "type2", empty ? "" : "C:/mypath\\field2x.txt", empty ? "" : "text2");
					MockFileItem fileItem3 = new MockFileItem("field3", null, null, "value3");
					MockFileItem fileItem4 = new MockFileItem("field4", "text/html; charset=iso-8859-1", null, "value4");
					MockFileItem fileItem5 = new MockFileItem("field4", null, null, "value5");
					fileItems.add(fileItem1);
					fileItems.add(fileItem1x);
					fileItems.add(fileItem2);
					fileItems.add(fileItem2x);
					fileItems.add(fileItem3);
					fileItems.add(fileItem4);
					fileItems.add(fileItem5);
					return fileItems;
				}
			};
		}
	}


	private static class MockFileItem implements FileItem {

		private String fieldName;
		private String contentType;
		private String name;
		private String value;

		private File writtenFile;
		private boolean deleted;

		public MockFileItem(String fieldName, String contentType, String name, String value) {
			this.fieldName = fieldName;
			this.contentType = contentType;
			this.name = name;
			this.value = value;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(value.getBytes());
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isInMemory() {
			return true;
		}

		@Override
		public long getSize() {
			return value.length();
		}

		@Override
		public byte[] get() {
			return value.getBytes();
		}

		@Override
		public String getString(String encoding) throws UnsupportedEncodingException {
			return new String(get(), encoding);
		}

		@Override
		public String getString() {
			return value;
		}

		@Override
		public void write(File file) throws Exception {
			this.writtenFile = file;
		}

		@Override
		public void delete() {
			this.deleted = true;
		}

		@Override
		public String getFieldName() {
			return fieldName;
		}

		@Override
		public void setFieldName(String s) {
			this.fieldName = s;
		}

		@Override
		public boolean isFormField() {
			return (this.name == null);
		}

		@Override
		public void setFormField(boolean b) {
			throw new UnsupportedOperationException();
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public FileItemHeaders getHeaders() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setHeaders(FileItemHeaders headers) {
			throw new UnsupportedOperationException();
		}
	}


	public class MultipartTestBean1 {

		private MultipartFile[] field1;
		private byte[] field2;

		public void setField1(MultipartFile[] field1) {
			this.field1 = field1;
		}

		public MultipartFile[] getField1() {
			return field1;
		}

		public void setField2(byte[] field2) {
			this.field2 = field2;
		}

		public byte[] getField2() {
			return field2;
		}
	}


	public class MultipartTestBean2 {

		private String[] field1;
		private String field2;

		public void setField1(String[] field1) {
			this.field1 = field1;
		}

		public String[] getField1() {
			return field1;
		}

		public void setField2(String field2) {
			this.field2 = field2;
		}

		public String getField2() {
			return field2;
		}
	}

}
