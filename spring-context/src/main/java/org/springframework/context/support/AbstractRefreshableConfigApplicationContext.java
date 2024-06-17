/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractRefreshableApplicationContext} subclass that adds common handling
 * of specified config locations. Serves as base class for XML-based application
 * context implementations such as {@link ClassPathXmlApplicationContext} and
 * {@link FileSystemXmlApplicationContext}, as well as
 * {@link org.springframework.web.context.support.XmlWebApplicationContext}.
 *
 * @author Juergen Hoeller
 * @see #setConfigLocation
 * @see #setConfigLocations
 * @see #getDefaultConfigLocations
 * @since 2.5.2
 */
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
		implements BeanNameAware, InitializingBean {

	/**
	 * 存储配置文件路径
	 */
	@Nullable
	private String[] configLocations;

	private boolean setIdCalled = false;


	/**
	 * Create a new AbstractRefreshableConfigApplicationContext with no parent.
	 */
	public AbstractRefreshableConfigApplicationContext() {
	}

	/**
	 * Create a new AbstractRefreshableConfigApplicationContext with the given parent context.
	 *
	 * @param parent the parent context
	 */
	public AbstractRefreshableConfigApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * Set the config locations for this application context in init-param style,
	 * i.e. with distinct locations separated by commas, semicolons or whitespace.
	 * <p>If not set, the implementation may use a default as appropriate.
	 */
	public void setConfigLocation(String location) {
		setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
	}

	/**
	 * Set the config locations for this application context.
	 * <p>If not set, the implementation may use a default as appropriate.
	 */
	public void setConfigLocations(@Nullable String... locations) {
		if (locations != null) {
			// Assert是Spring框架提供的的一个工具类，用于各种参数格式校验，这里是校验数组元素不能为null
			Assert.noNullElements(locations, "Config locations must not be null");
			// 初始化configLocations数组，长度为传入参数数组的length
			this.configLocations = new String[locations.length];
			for (int i = 0; i < locations.length; i++) {
				// 取出每一个配置路径字符串，并尝试替换字符串中的环境变量占位符，然后将结果设置到configLocations数组的对应位置中
				this.configLocations[i] = resolvePath(locations[i]).trim();
			}
		} else {
			this.configLocations = null;
		}
	}

	/**
	 * Return an array of resource locations, referring to the XML bean definition
	 * files that this context should be built with. Can also include location
	 * patterns, which will get resolved via a ResourcePatternResolver.
	 * <p>The default implementation returns {@code null}. Subclasses can override
	 * this to provide a set of resource locations to load bean definitions from.
	 *
	 * @return an array of resource locations, or {@code null} if none
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	@Nullable
	protected String[] getConfigLocations() {
		return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
	}

	/**
	 * Return the default config locations to use, for the case where no
	 * explicit config locations have been specified.
	 * <p>The default implementation returns {@code null},
	 * requiring explicit config locations.
	 *
	 * @return an array of default config locations, if any
	 * @see #setConfigLocations
	 */
	@Nullable
	protected String[] getDefaultConfigLocations() {
		return null;
	}

	/**
	 * 解析给定的路径，必要时用相应的环境属性值替换占位符
	 *
	 * @param path 传递的文件路径
	 * @return 解析后的文件路径
	 * @see org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)
	 */
	protected String resolvePath(String path) {
		// 1. getEnvironment方法获取（没有就创建）当前上下文的可配置环境变量对象（StandardEnvironment类型）
		// 2. 随后resolveRequiredPlaceholders方法替换字符串path中的${}占位符
		return getEnvironment().resolveRequiredPlaceholders(path);
	}


	@Override
	public void setId(String id) {
		super.setId(id);
		this.setIdCalled = true;
	}

	/**
	 * Sets the id of this context to the bean name by default,
	 * for cases where the context instance is itself defined as a bean.
	 */
	@Override
	public void setBeanName(String name) {
		if (!this.setIdCalled) {
			super.setId(name);
			setDisplayName("ApplicationContext '" + name + "'");
		}
	}

	/**
	 * Triggers {@link #refresh()} if not refreshed in the concrete context's
	 * constructor already.
	 */
	@Override
	public void afterPropertiesSet() {
		if (!isActive()) {
			refresh();
		}
	}

}
