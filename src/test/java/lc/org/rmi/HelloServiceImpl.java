package lc.org.rmi;

public class HelloServiceImpl implements HelloService {

	@Override
	public String select(String a, String b) {
		return a + " --- " + b;
	}
}
