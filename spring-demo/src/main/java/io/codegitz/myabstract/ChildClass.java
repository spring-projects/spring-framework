package io.codegitz.myabstract;

/**
 * @author 张观权
 * @date 2020/9/9 22:30
 **/
public class ChildClass extends ParentAbstractClass{
	@Override
	public void sayHello() {
		sayHi();
	}

	public static void main(String[] args) {
		ChildClass childClass = new ChildClass();
		childClass.sayHello();
	}
}
