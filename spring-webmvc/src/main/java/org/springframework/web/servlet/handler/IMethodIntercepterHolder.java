package org.springframework.web.servlet.handler;
/**
 * À¹½ØÆ÷Á´½Ó¿ÚÉùÃ÷
 * @author lehoon
 *
 */

import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;

public interface IMethodIntercepterHolder {
	public Object doChain(Method handlerMethod, Object handler,
			HttpServletRequest request, HttpServletResponse response,
			Model model) throws Exception;
}
