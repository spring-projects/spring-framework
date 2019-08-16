package lc.org.beans.factory;

import lc.org.beans.TestBean1th;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author : liuc
 * @date : 2019/4/16 10:33
 * @description :
 */
public class Tb1thFactoryBean implements FactoryBean<TestBean1th> {

	private String info;

	@Override
	public TestBean1th getObject() throws Exception {
		TestBean1th tb = new TestBean1th();
		if(this.info.length() > 0){
			String[] infos = this.info.split(",");
			tb.setName(infos[0]);
			tb.setAge(Integer.parseInt(infos[1]));
		}
		return tb;
	}

	@Override
	public Class<?> getObjectType() {
		return TestBean1th.class;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}
