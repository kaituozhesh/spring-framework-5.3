package org.springframework.testing.tag;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @Description:
 * @ClassName: DzgNamespaceHandler
 * @Auther: lin
 * @Date: 2024/6/5 20:16
 * @Version: 1.0
 */
public class DzgNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("user", new UserBeanDefinitionParser());
	}
}
