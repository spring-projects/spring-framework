package org.springframework.web.servlet.mvc.annotation;

/**
 * ¿πΩÿ∆˜π‹¿Ì’ﬂ
 * @author lehoon
 *
 */

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.support.HandlerMethodInvoker;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.handler.IMethodInvokerIntercepter;
import org.springframework.web.servlet.handler.IMethodIntercepterHolder;
import org.springframework.web.servlet.handler.MethodIntercepterHolder;
import org.springframework.web.servlet.mvc.support.MethodInvokerContext;

public class MethodInvokerIntercepterManager {
	IMethodIntercepterHolder root;
	private ThreadLocal<MethodInvokerContext> context = new ThreadLocal<MethodInvokerContext>();

	public ThreadLocal<MethodInvokerContext> getContext() {
		return context;
	}

	Object doChain(HandlerMethodInvoker invoker, Object handler,
			Method handlerMethod, ServletWebRequest webRequest,
			ExtendedModelMap model) throws Exception {
		if (root != null) {
			context.set(new MethodInvokerContext(invoker, webRequest, model));
			return root.doChain(handlerMethod, handler,
					webRequest.getRequest(), webRequest.getResponse(), model);
		}
		return invoker.invokeHandlerMethod(handlerMethod, handler, webRequest,
				model);
	}

	public void setIntercepters(List<IMethodInvokerIntercepter> intercepters) {
		if (intercepters != null && intercepters.size() > 0) {
			int size = intercepters.size();
			MethodIntercepterHolder holder = null;
			for (int i = 0; i < size; i++) {
				MethodIntercepterHolder previous = holder;
				IMethodInvokerIntercepter intercepter = intercepters.get(i);
				holder = new MethodIntercepterHolder(intercepter, this);
				if (previous != null) {
					previous.setNext(holder);
				} else {
					root = holder;
				}
			}
			holder.setNext(new MethodIntercepterHolder(holder, this));
		}
	}
}