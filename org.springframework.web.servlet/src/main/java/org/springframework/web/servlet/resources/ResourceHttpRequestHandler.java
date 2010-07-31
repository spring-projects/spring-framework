package org.springframework.web.servlet.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletContext;
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
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

/**
 * {@link HttpRequestHandler} that serves static resources optimized for superior browser performance 
 * (according to the guidelines of Page Speed, YSlow, etc.) by adding far future cache expiration headers.
 * 
 * <p>TODO - expand the docs further
 * 
 * @author Keith Donald
 * @author Jeremy Grelle
 * @since 3.0.4
 */
public class ResourceHttpRequestHandler implements HttpRequestHandler, ServletContextAware {

	private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

	private final List<Resource> resourcePaths;
	
	private final int maxAge = 31556926;
	
	private FileMediaTypeMap fileMediaTypeMap;
	
	public ResourceHttpRequestHandler(List<Resource> resourcePaths) {
		Assert.notNull(resourcePaths, "Resource paths must not be null");
		validateResourcePaths(resourcePaths);
		this.resourcePaths = resourcePaths;
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		if (!"GET".equals(request.getMethod())) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(),
					new String[] {"GET"}, "ResourceHttpRequestHandler only supports GET requests");
		}
		URLResource resource = getResource(request);
		if (resource == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (checkNotModified(resource, request, response)) {
			return;
		}
		prepareResponse(resource, response);
		writeResponse(resource, request, response);
	}
	
	public void setServletContext(ServletContext servletContext) {
		this.fileMediaTypeMap = new DefaultFileMediaTypeMap(servletContext);
	}
	
	private boolean checkNotModified(Resource resource,HttpServletRequest request, HttpServletResponse response) throws IOException {
		long ifModifiedSince = request.getDateHeader("If-Modified-Since");					
		boolean notModified = ifModifiedSince >= (resource.lastModified() / 1000 * 1000);
		if (notModified) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			response.setDateHeader("Last-Modified", resource.lastModified());
		}
		return notModified;
	}

	private URLResource getResource(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" + HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");			
		}
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			return null;
		}
		for (Resource resourcePath : this.resourcePaths) {
			Resource resource;
			try {
				resource = resourcePath.createRelative(path);
				if (isValidFile(resource)) {
					return new URLResource(resource, fileMediaTypeMap.getMediaType(resource.getFilename()));
				}
			} catch (IOException e) {
				//Resource not found
				return null;
			}
		}
		return null;
	}
	
	private void prepareResponse(URLResource resource, HttpServletResponse response) throws IOException {
		response.setContentType(resource.getMediaType().toString());
		response.setContentLength(resource.getContentLength());
		response.setDateHeader("Last-Modified", resource.lastModified());
		if (this.maxAge > 0) {
			// HTTP 1.0 header
			response.setDateHeader("Expires", System.currentTimeMillis() + this.maxAge * 1000L);
			// HTTP 1.1 header
			response.setHeader("Cache-Control", "max-age=" + this.maxAge);
		}
	}

	private void writeResponse(URLResource resource, HttpServletRequest request, HttpServletResponse response) throws IOException {
		OutputStream out = response.getOutputStream();
		try {
			InputStream in = resource.getInputStream();
			try {
				byte[] buffer = new byte[1024];
				int bytesRead = -1;
				while ((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	private boolean isValidFile(Resource resource) throws IOException {
		return resource.exists() && StringUtils.hasText(resource.getFilename());
	}
	
	private void validateResourcePaths(List<Resource> resourcePaths) {
		for (Resource path : resourcePaths) {
			Assert.isTrue(path.exists(), path.getDescription() + " is not a valid resource location as it does not exist.");
			Assert.isTrue(!StringUtils.hasText(path.getFilename()), path.getDescription()+" is not a valid resource location.  Resource paths must end with a '/'.");
		}		
	}
		
	private interface FileMediaTypeMap {
		MediaType getMediaType(String fileName);
	}
	
	private static class DefaultFileMediaTypeMap implements FileMediaTypeMap {

		private static final boolean jafPresent =
			ClassUtils.isPresent("javax.activation.FileTypeMap", ContentNegotiatingViewResolver.class.getClassLoader());

		private ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<String, MediaType>();
		
		private final ServletContext servletContext;
		
		public DefaultFileMediaTypeMap(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		public MediaType getMediaType(String filename) {
			String extension = StringUtils.getFilenameExtension(filename);
			if (!StringUtils.hasText(extension)) {
				return null;
			}
			extension = extension.toLowerCase(Locale.ENGLISH);
			MediaType mediaType = this.mediaTypes.get(extension);
			if (mediaType == null) {
				String mimeType = servletContext.getMimeType(filename);
				if (StringUtils.hasText(mimeType)) {
					mediaType = MediaType.parseMediaType(mimeType);
				}
			}
			if (mediaType == null && jafPresent) {
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
	
	private static class URLResource implements Resource {
		
		private final Resource wrapped;
		
		private final long lastModified;
		
		private final int contentLength;
		
		private final MediaType mediaType;
		
		public URLResource(Resource wrapped, MediaType mediaType) throws IOException {
			this.wrapped = wrapped;
			URLConnection connection = null;
			try {
				connection = wrapped.getURL().openConnection();
				this.lastModified = connection.getLastModified();
				this.contentLength = connection.getContentLength();
				this.mediaType = mediaType;
			} finally {
				if (connection != null) {
					connection.getInputStream().close();
				}
			}
		}
		
		public int getContentLength() {
			return this.contentLength;
		}
		
		public MediaType getMediaType() {
			return mediaType;
		}

		public long lastModified() throws IOException {
			return this.lastModified;
		}

		public Resource createRelative(String relativePath) throws IOException {
			return wrapped.createRelative(relativePath);
		}

		public boolean exists() {
			return wrapped.exists();
		}

		public String getDescription() {
			return wrapped.getDescription();
		}

		public File getFile() throws IOException {
			return wrapped.getFile();
		}

		public String getFilename() {
			return wrapped.getFilename();
		}

		public URI getURI() throws IOException {
			return wrapped.getURI();
		}

		public URL getURL() throws IOException {
			return wrapped.getURL();
		}

		public boolean isOpen() {
			return wrapped.isOpen();
		}

		public boolean isReadable() {
			return wrapped.isReadable();
		}

		public InputStream getInputStream() throws IOException {
			return wrapped.getInputStream();
		}		
	}
}
