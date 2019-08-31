package lc.org.beans;

import org.springframework.aop.framework.AopContext;

/**
 * @author : liuc
 * @date : 2019/4/8 10:54
 * @description :
 */
public class TestBean1th implements ITestBean{

	private String name = "lc";

	private int age;

	private TestBean2th testBean2th;

	public TestBean2th getTestBean2th() {
		return testBean2th;
	}

	public void setTestBean2th(TestBean2th testBean2th) {
		this.testBean2th = testBean2th;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public void m(String a1){
		System.out.println("TestBean1th : " + a1);
	}

	@Override
	public String toString() {
		return "this is a TestBean1th Object";
	}

	@Override
	public void test(){
		System.out.println("this is a test method...");
	}

	public void test2(){
		System.out.println("this is a 2 test method");
		//((TestBean1th) AopContext.currentProxy()).test();
		//this.test();
		if(testBean2th != null ){
			testBean2th.test();
		}
	}

	public void t(){
		System.out.println("this is a t method");
	}
}
