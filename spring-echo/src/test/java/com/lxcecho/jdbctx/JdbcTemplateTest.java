package com.lxcecho.jdbctx;

import com.lxcecho.jdbctx.jdbc.Emp;
import com.lxcecho.jdbctx.tx.config.TxConfig;
import com.lxcecho.jdbctx.tx.controller.BookController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@SpringJUnitConfig(locations = "classpath:beans.xml")
public class JdbcTemplateTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	//查询：返回对象
	@Test
	public void testSelectObject() {
		//写法一
		//        String sql = "select * from t_emp where id=?";
		//        Emp empResult = jdbcTemplate.queryForObject(sql,
		//                (rs, rowNum) -> {
		//                    Emp emp = new Emp();
		//                    emp.setId(rs.getInt("id"));
		//                    emp.setName(rs.getString("name"));
		//                    emp.setAge(rs.getInt("age"));
		//                    emp.setSex(rs.getString("sex"));
		//                    return emp;
		//                }, 1);
		//        System.out.println(empResult);

		//写法二
		String sql = "select * from t_emp where id=?";
		Emp emp = jdbcTemplate.queryForObject(sql,
				new BeanPropertyRowMapper<>(Emp.class), 1);
		System.out.println(emp);
	}

	//查询：返回list集合
	@Test
	public void testSelectList() {
		String sql = "select * from t_emp";
		List<Emp> list = jdbcTemplate.query(sql,
				new BeanPropertyRowMapper<>(Emp.class));
		System.out.println(list);
	}

	//查询：返回单个值
	@Test
	public void testSelectValue() {
		String sql = "select count(*) from t_emp";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
		System.out.println(count);
	}

	//添加  修改  删除操作
	@Test
	public void testUpdate() {
        /*
        //1 添加操作
        //第一步 编写sql语句
        String sql = "INSERT INTO t_emp VALUES(NULL,?,?,?)";
        //第二步 调用jdbcTemplate的方法，传入相关参数
//        Object[] params = {"东方不败", 20, "未知"};
//        int rows = jdbcTemplate.update(sql,params);
        int rows = jdbcTemplate.update(sql,"林平之", 20, "未知");
        System.out.println(rows);
        */

        /*
        //2 修改操作
        String sql = "update t_emp set name=? where id=?";
        int rows = jdbcTemplate.update(sql,"林平之atguigu",3);
        System.out.println(rows);
         */

		//3 删除操作
		String sql = "delete from t_emp where id=?";
		int rows = jdbcTemplate.update(sql, 3);
		System.out.println(rows);
	}

	@Test
	public void testTxAllAnnotation() {
		ApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(TxConfig.class);
		BookController accountService = applicationContext.getBean("bookController", BookController.class);
		Integer[] bookIds = {1, 2};
		accountService.checkout(bookIds, 1);
	}

}
