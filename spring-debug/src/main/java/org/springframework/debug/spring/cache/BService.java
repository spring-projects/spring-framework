package org.springframework.debug.spring.cache;

/**
 * @Author: zhoudong
 * @Description:
 * @Date: 2022/7/20 19:09
 * @Version:
 **/
public class BService {

	private AService aService;

	public AService getaService() {
		return aService;
	}

	public void setaService(AService aService) {
		this.aService = aService;
	}
}
