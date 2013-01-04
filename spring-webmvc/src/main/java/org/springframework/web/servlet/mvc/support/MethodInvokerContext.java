package org.springframework.web.servlet.mvc.support;
/**
 * 
 * @author lehoon
 *
 */

import org.springframework.web.bind.annotation.support.HandlerMethodInvoker;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.ui.ExtendedModelMap;
public class MethodInvokerContext {
	public HandlerMethodInvoker invoker;
	public ServletWebRequest webRequest;
	public ExtendedModelMap model;
	public MethodInvokerContext(HandlerMethodInvoker invoker,
	        ServletWebRequest webRequest, ExtendedModelMap model) {
	    this.invoker = invoker;
	    this.webRequest = webRequest;
	    this.model = model;
	}
}
