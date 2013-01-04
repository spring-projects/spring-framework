package org.springframework.web.servlet.handler;

/**
 * 
 */
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.annotation.MethodInvokerIntercepterManager;
import org.springframework.web.servlet.mvc.support.MethodInvokerContext;
/**
 * À¹½ØÆ÷°ü¹üÈÝÆ÷
 * @author lehoon
 *
 */
public class MethodIntercepterHolder implements IMethodIntercepterHolder,
		IMethodInvokerIntercepter {
	MethodInvokerIntercepterManager manager;
	IMethodInvokerIntercepter self;
	IMethodIntercepterHolder next;

	public MethodIntercepterHolder(IMethodInvokerIntercepter self,
			MethodInvokerIntercepterManager manager) {
		this.self = self;
		this.manager = manager;
	}

	public void setNext(IMethodIntercepterHolder next) {
		this.next = next;
	}

	@Override
	public Object doChain(Method handlerMethod, Object handler,
			HttpServletRequest request, HttpServletResponse response,
			Model model) throws Exception {
		return self.invokeHandlerMethod(handlerMethod, handler, request,
				response, model, next);
	}

	@Override
	public Object invokeHandlerMethod(Method handlerMethod, Object handler,
			HttpServletRequest request, HttpServletResponse response,
			Model model, IMethodIntercepterHolder chain) throws Exception {
		MethodInvokerContext ctx = manager.getContext().get();
		manager.getContext().remove();
		return ctx.invoker.invokeHandlerMethod(handlerMethod, handler,
				ctx.webRequest, ctx.model);
	}

}