/*
 * Copyright 2024 the original author or authors.
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
package com.sample.sgready.config;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.util.Set;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.stereotype.Component;

/**
 * {@link org.springframework.core.convert.converter.Converter} to convert strings and numbers to {@link Quantity}.
 *
 * @author Mark Paluch
 */
@Component
@ConfigurationPropertiesBinding
public class QuantityConverter implements ConditionalGenericConverter {

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return targetType.getType().equals(Quantity.class);
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		ResolvableType component = targetType.getResolvableType().getGeneric(0);
		Unit<?> unit = Units.getInstance().getUnit((Class) component.resolve());

		try {
			return Quantities.getQuantity(Integer.parseInt(source.toString()), unit);
		} catch (NumberFormatException ignored) {}

		try {
			return Quantities.getQuantity(Long.parseLong(source.toString()), unit);
		} catch (NumberFormatException ignored) {}

		try {
			return Quantities.getQuantity(Double.parseDouble(source.toString()), unit);
		} catch (NumberFormatException ignored) {}

		return Quantities.getQuantity(source.toString());
	}
}
