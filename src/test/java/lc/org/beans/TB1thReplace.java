package lc.org.beans;

import org.springframework.beans.factory.support.MethodReplacer;

import java.lang.reflect.Method;

/**
 * @author : liuc
 * @date : 2019/4/9 16:50
 * @description :
 */
public class TB1thReplace implements MethodReplacer {

	private int i = 1;
	@Override
	public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
		System.out.println("replace : " + args[0]);
		// 死循环
		if(!(i > 2)){
			method.invoke(obj,String.valueOf(++i));
		}
		System.out.println();
		return null;
	}
}
