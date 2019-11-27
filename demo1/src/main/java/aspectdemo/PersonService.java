package aspectdemo;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class PersonService implements InitializingBean, DisposableBean {
	private String name;

	public String getPersonName(){
		System.out.println("-------getPersonName-------:"+ name);
		return name;
	}

	public boolean setPersonName(String name){
		System.out.println("--------setPersonName------:"+name);
		this.name = name;
		return true;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("afterPropertiesSet----->");
		this.name = "kobe";
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("destroy-------->");
	}
}
