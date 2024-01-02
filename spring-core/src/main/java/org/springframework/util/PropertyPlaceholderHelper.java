/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * Utility class for working with Strings that have placeholder values in them.
 * A placeholder takes the form {@code ${name}}. Using {@code PropertyPlaceholderHelper}
 * these placeholders can be substituted for user-supplied values.
 *
 * <p>Values for substitution can be supplied using a {@link Properties} instance or
 * using a {@link PlaceholderResolver}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 3.0
 */
public class PropertyPlaceholderHelper {

	private static final Log logger = LogFactory.getLog(PropertyPlaceholderHelper.class);

	private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);

	static {
		wellKnownSimplePrefixes.put("}", "{");
		wellKnownSimplePrefixes.put("]", "[");
		wellKnownSimplePrefixes.put(")", "(");
	}


	private final String placeholderPrefix;

	private final String placeholderSuffix;

	private final String simplePrefix;

	@Nullable
	private final String valueSeparator;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * Creates a new {@code PropertyPlaceholderHelper} that uses the supplied prefix and suffix.
	 * Unresolvable placeholders are ignored.
	 *
	 * @param placeholderPrefix the prefix that denotes the start of a placeholder
	 * @param placeholderSuffix the suffix that denotes the end of a placeholder
	 */
	public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
		this(placeholderPrefix, placeholderSuffix, null, true);
	}

	/**
	 * 创建属性占位符辅助对象 设置占位符的解析格式 默认就是 ${...:...}
	 *
	 * @param placeholderPrefix              表示占位符开头的前缀
	 * @param placeholderSuffix              表示占位符结尾的后缀
	 * @param valueSeparator                 占位符变量和关联的默认值之间的分隔字符（如果有）
	 * @param ignoreUnresolvablePlaceholders 指示是否应忽略无法解析的占位符，true 忽略 false 抛出异常
	 */
	public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix,
									 @Nullable String valueSeparator, boolean ignoreUnresolvablePlaceholders) {

		Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
		Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
		this.placeholderPrefix = placeholderPrefix;
		this.placeholderSuffix = placeholderSuffix;
		String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
		if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
			this.simplePrefix = simplePrefixForSuffix;
		} else {
			this.simplePrefix = this.placeholderPrefix;
		}
		this.valueSeparator = valueSeparator;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	/**
	 * Replaces all placeholders of format {@code ${name}} with the corresponding
	 * property from the supplied {@link Properties}.
	 *
	 * @param value      the value containing the placeholders to be replaced
	 * @param properties the {@code Properties} to use for replacement
	 * @return the supplied value with placeholders replaced inline
	 */
	public String replacePlaceholders(String value, final Properties properties) {
		Assert.notNull(properties, "'properties' must not be null");
		return replacePlaceholders(value, properties::getProperty);
	}

	/**
	 * Replaces all placeholders of format {@code ${name}} with the value returned
	 * from the supplied {@link PlaceholderResolver}.
	 *
	 * @param value               the value containing the placeholders to be replaced
	 * @param placeholderResolver the {@code PlaceholderResolver} to use for replacement
	 * @return the supplied value with placeholders replaced inline
	 */
	public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
		Assert.notNull(value, "'value' must not be null");
		return parseStringValue(value, placeholderResolver, null);
	}

	/**
	 * 递归解析字符串中的占位符
	 *
	 * @param value               包含要替换的占位符的值
	 * @param placeholderResolver 占位符解析器
	 * @param visitedPlaceholders 访问过的占位符，用于地位向后推进，同时避免递归解析导致的死循环
	 * @return 替换之后的值
	 */
	protected String parseStringValue(
			String value, PlaceholderResolver placeholderResolver, @Nullable Set<String> visitedPlaceholders) {

		// 获取第一个占位符前缀索引值 startIndex
		int startIndex = value.indexOf(this.placeholderPrefix);
		// 字符串没有占位符了，则原值返回
		if (startIndex == -1) {
			return value;
		}

		// 保存解析前的值
		StringBuilder result = new StringBuilder(value);
		// 一直循环直到将全部占位符都解析完毕或者抛出异常
		while (startIndex != -1) {
			// 获取对应占位符结束位置下标
			int endIndex = findPlaceholderEndIndex(result, startIndex);
			if (endIndex != -1) {
				// 获取占位符中间的内容
				String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
				String originalPlaceholder = placeholder;
				// 添加到已解析的占位符中
				if (visitedPlaceholders == null) {
					visitedPlaceholders = new HashSet<>(4);
				}
				// 如果没有添加成功，这说明出现了同名的的嵌套占位符，这类似于循环引用，那么抛出异常
				if (!visitedPlaceholders.add(originalPlaceholder)) {
					throw new IllegalArgumentException(
							"Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
				}
				// 递归调用parseStringValue，用于分析占位符中的占位符，这里是用于分析key
				placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
				// 到这里表示递归完毕，占位符中没有占位符了
				// 找到最底层的占位符变量之后，调用placeholderResolver的resolvePlaceholder(placeholder)方法，根据lambda表达式
				// 实际上就是调用PropertySourcesPropertyResolver对象的getPropertyAsRawString方法，然后又会调用
				// PropertySourcesPropertyResolver.getProperty方法中通过占位符变量找出对应的值
				String propVal = placeholderResolver.resolvePlaceholder(placeholder);
				// 如果没有找到，并且默认值分隔符不为null，那么尝试获取默认值
				if (propVal == null && this.valueSeparator != null) {
					// 分隔符的起始下标
					int separatorIndex = placeholder.indexOf(this.valueSeparator);
					if (separatorIndex != -1) {
						// 实际的占位符变量
						String actualPlaceholder = placeholder.substring(0, separatorIndex);
						// 默认值
						String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
						// 尝试从属性源中查找占位符变量对应的属性值
						propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
						// 如果没有找到就使用默认值
						if (propVal == null) {
							propVal = defaultValue;
						}
					}
				}
				// 解析到了值
				if (propVal != null) {
					// 递归调用parseStringValue方法，这里是解析值中是否有占位符
					propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
					// 递归结束，将占位符替换为对应的值
					result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
					if (logger.isTraceEnabled()) {
						logger.trace("Resolved placeholder '" + placeholder + "'");
					}
					// 获取下一个占位符的起始索引
					startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
				}
				// 如果值为null，并且默认值分隔符为null，这就是解析失败的情况，判断是不是忽略，如果是的话，那么解析下一个占位符，同时将当前占位符整体作为值
				else if (this.ignoreUnresolvablePlaceholders) {
					// Proceed with unprocessed value.
					startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
				}
				// 不能忽略则抛出异常
				else {
					throw new IllegalArgumentException("Could not resolve placeholder '" +
							placeholder + "'" + " in value \"" + value + "\"");
				}
				// 移除已解析的占位符变量
				visitedPlaceholders.remove(originalPlaceholder);
			} else {
				// 出现问题。没有找到后缀，下次循环直接退出
				startIndex = -1;
			}
		}
		return result.toString();
	}

	private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
		int index = startIndex + this.placeholderPrefix.length();
		int withinNestedPlaceholder = 0;
		while (index < buf.length()) {
			if (StringUtils.substringMatch(buf, index, this.placeholderSuffix)) {
				if (withinNestedPlaceholder > 0) {
					withinNestedPlaceholder--;
					index = index + this.placeholderSuffix.length();
				} else {
					return index;
				}
			} else if (StringUtils.substringMatch(buf, index, this.simplePrefix)) {
				withinNestedPlaceholder++;
				index = index + this.simplePrefix.length();
			} else {
				index++;
			}
		}
		return -1;
	}


	/**
	 * Strategy interface used to resolve replacement values for placeholders contained in Strings.
	 */
	@FunctionalInterface
	public interface PlaceholderResolver {

		/**
		 * Resolve the supplied placeholder name to the replacement value.
		 *
		 * @param placeholderName the name of the placeholder to resolve
		 * @return the replacement value, or {@code null} if no replacement is to be made
		 */
		@Nullable
		String resolvePlaceholder(String placeholderName);
	}

}
