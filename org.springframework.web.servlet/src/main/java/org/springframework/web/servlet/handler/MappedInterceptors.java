package org.springframework.web.servlet.handler;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

class MappedInterceptors {
	
	private MappedInterceptor[] mappedInterceptors;

	public MappedInterceptors(MappedInterceptor[] mappedInterceptors) {
		this.mappedInterceptors = mappedInterceptors;
	}
	
	public Set<HandlerInterceptor> getInterceptors(String lookupPath, PathMatcher pathMatcher) {
		Set<HandlerInterceptor> interceptors = new LinkedHashSet<HandlerInterceptor>();
		for (MappedInterceptor interceptor : this.mappedInterceptors) {
			if (matches(interceptor, lookupPath, pathMatcher)) {
				interceptors.add(interceptor.getInterceptor());				
			}
		}
		return interceptors;
	}
	
	private boolean matches(MappedInterceptor interceptor, String lookupPath, PathMatcher pathMatcher) {
		String[] pathPatterns = interceptor.getPathPatterns();
		if (pathPatterns != null) {
			for (String pattern : pathPatterns) {
				if (pathMatcher.match(pattern, lookupPath)) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
}
