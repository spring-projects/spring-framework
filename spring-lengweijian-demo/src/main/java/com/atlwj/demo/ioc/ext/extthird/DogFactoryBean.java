package com.atlwj.demo.ioc.ext.extthird;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * 这是一个创建狗狗的工厂bean
 * 需求：
 * 		1.狗狗的昵称：狗狗的小主任叫他tom，其他大人叫他大黄。
 * 		2.狗狗的孩子：黄颜色的叫smallYelloDog,黑颜色的叫smallBlackDog
 * 	    3.狗狗的主人：小孩叫lucy，lucy的父母叫lucyParents	.
 * 	    4.狗狗的性格：家里没人的时候就拆家，没有人的时候就喜欢睡觉。
 */
@Component
public class DogFactoryBean implements FactoryBean<Dog> {



	@Override
	public Dog getObject() throws Exception {
		return getDog();
	}

	/**
	 * 创建狗狗instance
	 * @return
	 */
	private Dog getDog() {
		// 1.狗狗的昵称(nickName)：狗狗的小主任叫他tom，其他大人叫他大黄。
		// 2.狗狗的孩子(sonDogs)：黄颜色的叫smallYelloDog,黑颜色的叫smallBlackDog
		// 3.狗狗的主人(hostMan)：小孩叫lucy，lucy的父母叫lucyParents	.
		// 4.狗狗的性格(character)：家里没人的时候就拆家，没有人的时候就喜欢睡觉。
		Dog dog = new Dog();
		/**
		 * 伪代码：
		 * 		if (smallPerson) {
		 * 		    nickName = "tom"
		 * 		}else {
		 * 		    nickName = "大黄"
		 * 		}
		 *
		 *
		 */
		dog.setNickName("tom");

		/**
		 * 伪代码：
		 * 		if (大人) {
		 * 		    HostMan = "lucyParents"
		 * 		}
		 * 		if (小孩) {
		 * 		     HostMan = "lucy"
		 * 		}
		 */
		dog.setHostMan("lucy");

		/**
		 * 为代码：
		 * 		if (黄色) {
		 * 		    SonDogs[0] = smallYelloDog
		 * 		}else if (黑色) {
		 * 		    SonDogs[1] = smallBlackDog
		 * 		}
		 */
		dog.setSonDogs(new String[]{"smallYelloDog","smallBlackDog"});

		/**
		 * 伪代码：
		 * 		when (家里没人) {
		 * 		 	character = "喜欢拆家"
		 * 		}
		 * 	    when (家里有人) {
		 * 	        character = "喜欢睡觉"
		 * 	    }
		 */
		dog.setCharacter("喜欢睡觉");
		return dog;
	}

	@Override
	public Class<?> getObjectType() {
		return Dog.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
