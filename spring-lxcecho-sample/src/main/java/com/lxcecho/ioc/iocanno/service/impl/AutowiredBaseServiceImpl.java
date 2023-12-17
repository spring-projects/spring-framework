package com.lxcecho.ioc.iocanno.service.impl;

import com.lxcecho.ioc.iocanno.dao.BaseDao;
import com.lxcecho.ioc.iocanno.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Service
public class AutowiredBaseServiceImpl implements BaseService {

	// 注入 dao

	/**
	 * 第一种方式  属性注入
	 */
    @Autowired  //根 据类型找到对应对象，完成注入
    private BaseDao baseDao;

	/**
	 * 第二种方式 set 方法注入
	 */
    /*private BaseDao baseDao;
    @Autowired
    public void setBaseDao(BaseDao baseDao) {
        this.baseDao = baseDao;
    }*/

	/**
	 * 第三种方式  构造方法注入
	 */
    /*private BaseDao baseDao;
    @Autowired
    public AutowiredBaseServiceImpl(BaseDao baseDao) {
        this.baseDao = baseDao;
    }*/

	/**
	 * 第四种方式 形参上注入
	 */
    /*private BaseDao baseDao;
    public AutowiredBaseServiceImpl(@Autowired BaseDao baseDao) {
        this.baseDao = baseDao;
    }*/

	/**
	 * 最后方式： 两个注解，根据名称注入
	 */
	/*@Autowired
	@Qualifier(value = "redisDaoImpl")
	private BaseDao baseDao;*/

	@Override
	public void add() {
		System.out.println("AutowiredBaseServiceImpl.....");
		baseDao.add();
	}
}
