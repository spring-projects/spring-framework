package initbean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class Person  {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void initPerson(){
		System.out.println("initPerson");
		this.name = "kobe";
	}

	public void destroyPerson(){
		System.out.println("destoryPerson");
	}
}
