package org.springframework.aop.framework;

/**
 * The final class can not proxy by cglib. So return the target object.
 * @author: caoxuhao
 * @Date: 2021/5/10 14:09
 */
public class NeedNotProxy implements AopProxy{

	Object target;

	public NeedNotProxy(Object target){
		this.target = target;
	}

	@Override
	public Object getProxy() {
		return target;
	}

	@Override
	public Object getProxy(ClassLoader classLoader) {
		return target;
	}
}
