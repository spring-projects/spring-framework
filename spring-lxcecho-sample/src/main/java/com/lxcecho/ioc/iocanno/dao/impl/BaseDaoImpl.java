package com.lxcecho.ioc.iocanno.dao.impl;

import com.lxcecho.ioc.iocanno.dao.BaseDao;
import org.springframework.stereotype.Repository;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Repository("redisDaoImpl")
public class BaseDaoImpl implements BaseDao {

	@Override
	public void add() {
		System.out.println("BaseDaoImpl redis.........");
	}

}
