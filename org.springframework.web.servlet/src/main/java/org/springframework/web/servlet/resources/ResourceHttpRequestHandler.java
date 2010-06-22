package org.springframework.web.servlet.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

public class ResourceHttpRequestHandler implements HttpRequestHandler {

	private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

	private Resource resourceDirectory;
	
	private int maxAge = 31556926;
	
	private FileMediaTypeMap fileMediaTypeMap = new DefaultFileMediaTypeMap();
	
	public ResourceHttpRequestHandler(Resource resourceDirectory) {
		Assert.notNull(resourceDirectory, "The resource directory may not be null");
		this.resourceDirectory = resourceDirectory;
	}
	
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!"GET".equals(request.getMethod())) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(),
					new String[] {"GET"}, "ResourceHttpRequestHandler only supports GET requests");
		}
		List<Resource> resources = getResources(request);
		if (resources.size() == 0) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		boolean notModified = checkNotModified(resources, request, response);
		if (notModified) {
			return;
		}
		prepareResponse(resources, response);
		writeResponse(resources, response);
	}

	private List<Resource> getResources(HttpServletRequest request) throws ServletException, IOException {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" + HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");			
		}
		String[] resourceElements = path.split(",");
		if (resourceElements.length == 1 && resourceElements[0].length() == 0) {
			throw new NoSuchRequestHandlingMethodException(request);
		}
		List<Resource> resources = new ArrayList<Resource>();
		String[] dirAndFilename = splitDirectoryAndFilename(resourceElements[0]);
		String dir = dirAndFilename[0];
		String filename = dirAndFilename[1];
		Resource parent = dir != null ? this.resourceDirectory.createRelative(dir) : this.resourceDirectory;
		addResource(parent, filename, resources);
		if (resourceElements.length > 1) {
			for (int i = 1; i < resourceElements.length; i++) {
				addResource(parent, resourceElements[i], resources);
			}					
		}
		return resources;
	}
	
	private boolean checkNotModified(List<Resource> resources,HttpServletRequest request, HttpServletResponse response) throws IOException {
		long lastModifiedTimestamp = -1;
		long ifModifiedSince = request.getDateHeader("If-Modified-Since");			
		for (Resource resource : resources) {
			long resourceLastModified = resource.lastModified();
			if (resourceLastModified > lastModifiedTimestamp) {
				lastModifiedTimestamp = resourceLastModified;
			}				
		}
		boolean notModified = ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000);
		if (notModified) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			response.setDateHeader("Last-Modified", lastModifiedTimestamp);
		}
		return notModified;
	}
	
	private void prepareResponse(List<Resource> resources, HttpServletResponse response) {
		MediaType mediaType = null;
		int contentLength = 0;
		for (Resource resource : resources) {
			try {
				File file = resource.getFile();
				if (mediaType == null) {
					mediaType = fileMediaTypeMap.getMediaType(file.getName());
				}
				contentLength += file.length();				
			} catch (IOException e) {
				
			}
		}
		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}
		response.setContentLength(contentLength);
		if (this.maxAge > 0) {
			// HTTP 1.0 header
			response.setDateHeader("Expires", System.currentTimeMillis() + this.maxAge * 1000L);
			// HTTP 1.1 header
			response.setHeader("Cache-Control", "max-age=" + this.maxAge);
		}
	}
	
	private void writeResponse(List<Resource> resources, HttpServletResponse response) throws IOException {
		for (Resource resource : resources) {
			InputStream in = null;
			try {
				in = resource.getInputStream();
				int bytesRead = -1;
				byte[] buffer = new byte[4096];
				while ((bytesRead = in.read(buffer)) != -1) {
					response.getOutputStream().write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						
					}
				}
			}
		}
	}
	
	private String[] splitDirectoryAndFilename(String firstResourceElement) {
		int index = firstResourceElement.lastIndexOf("/");
		String dir;
		if (index == -1) {
			dir = null;
		} else {
			dir = firstResourceElement.substring(0, index + 1);
		}
		String filename = firstResourceElement.substring(index + 1, firstResourceElement.length());
		return new String[] { dir, filename };
	}
	
	private void addResource(Resource parent, String name, List<Resource> resources) throws IOException {
		if (name.length() > 0) {
			Resource resource = parent.createRelative(name);
			if (isAllowed(resource)) {
				resources.add(resource);
			}
		}
	}
	
	private boolean isAllowed(Resource resource) throws IOException {
		return resource.exists() && resource.getFile().isFile();
	}
		
	// TODO promote to top-level and make reusable
	// TODO check ServletContext.getMimeType(String) first
	
	public interface FileMediaTypeMap {
		MediaType getMediaType(String fileName);
	}
	
	public static class DefaultFileMediaTypeMap implements FileMediaTypeMap {

		private static final boolean jafPresent =
			ClassUtils.isPresent("javax.activation.FileTypeMap", ContentNegotiatingViewResolver.class.getClassLoader());

		private boolean useJaf = true;

		private ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<String, MediaType>();

		public MediaType getMediaType(String filename) {
			String extension = StringUtils.getFilenameExtension(filename);
			if (!StringUtils.hasText(extension)) {
				return null;
			}
			extension = extension.toLowerCase(Locale.ENGLISH);
			MediaType mediaType = this.mediaTypes.get(extension);
			if (mediaType == null && useJaf && jafPresent) {
				mediaType = ActivationMediaTypeFactory.getMediaType(filename);
				if (mediaType != null) {
					this.mediaTypes.putIfAbsent(extension, mediaType);
				}
			}
			return mediaType;
		}
		
		/**
		 * Inner class to avoid hard-coded JAF dependency.
		 */
		private static class ActivationMediaTypeFactory {

			private static final FileTypeMap fileTypeMap;

			static {
				fileTypeMap = loadFileTypeMapFromContextSupportModule();
			}

			private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
				// see if we can find the extended mime.types from the context-support module
				Resource mappingLocation = new ClassPathResource("org/springframework/mail/javamail/mime.types");
				if (mappingLocation.exists()) {
					if (logger.isTraceEnabled()) {
						logger.trace("Loading Java Activation Framework FileTypeMap from " + mappingLocation);
					}
					InputStream inputStream = null;
					try {
						inputStream = mappingLocation.getInputStream();
						return new MimetypesFileTypeMap(inputStream);
					}
					catch (IOException ex) {
						// ignore
					}
					finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							}
							catch (IOException ex) {
								// ignore
							}
						}
					}
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Loading default Java Activation Framework FileTypeMap");
				}
				return FileTypeMap.getDefaultFileTypeMap();
			}

			public static MediaType getMediaType(String fileName) {
				String mediaType = fileTypeMap.getContentType(fileName);
				return StringUtils.hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null;
			}
		}
	}

}
