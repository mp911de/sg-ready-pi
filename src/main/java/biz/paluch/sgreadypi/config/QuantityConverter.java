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
package biz.paluch.sgreadypi.config;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.util.Map;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.stereotype.Component;

/**
 * Converts configuration values to {@link Quantity}. The unit a bare number is interpreted in is resolved explicitly
 * from the target {@code Quantity
 * <Q>} dimension via {@link #DEFAULT_UNITS}, so a {@code Quantity<Power>} reads as watts and a
 * {@code Quantity<Dimensionless>} reads as percent. An unmapped dimension fails fast instead of guessing, and a value
 * that already carries a unit (for example {@code "2 kW"} or {@code "65 %"}) is parsed as-is and validated against the
 * expected dimension.
 * <p>
 * The dimension is read from {@link TypeDescriptor#getResolvableType()}, which carries the generic for both
 * setter-bound fields and constructor-bound record components (for example
 * {@link biz.paluch.sgreadypi.SgReadyProperties.Levels}).
 *
 * @author Mark Paluch
 */
@Component
@ConfigurationPropertiesBinding
public class QuantityConverter implements ConditionalGenericConverter {

	/**
	 * Default unit used to interpret a bare numeric configuration value for each supported quantity dimension. Extend
	 * this map to support additional dimensions; values carrying an explicit unit do not depend on it.
	 */
	private static final Map<Class<?>, Unit<?>> DEFAULT_UNITS = Map.of( //
			Power.class, Units.WATT, //
			Dimensionless.class, Units.PERCENT);

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return Quantity.class.equals(targetType.getType());
	}

	@Override
	public @Nullable Set<ConvertiblePair> getConvertibleTypes() {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (source == null) {
			return null;
		}

		String value = source.toString().trim();
		Class<?> dimension = resolveDimension(targetType, value);
		Unit<?> defaultUnit = DEFAULT_UNITS.get(dimension);

		if (defaultUnit == null) {
			throw new IllegalArgumentException(
					"Unsupported quantity dimension %s for value '%s'. Register a default unit in QuantityConverter or provide an explicit unit in the value."
							.formatted(dimension.getSimpleName(), value));
		}

		Number number = parseNumber(value);
		if (number != null) {
			return Quantities.getQuantity(number, defaultUnit);
		}

		// value carries an explicit unit, for example "2 kW" or "65 %"
		Quantity<?> quantity = Quantities.getQuantity(value);
		if (!quantity.getUnit().isCompatible(defaultUnit)) {
			throw new IllegalArgumentException(
					"Quantity '%s' is not compatible with the expected %s dimension".formatted(value, dimension.getSimpleName()));
		}
		return quantity;
	}

	private static Class<?> resolveDimension(TypeDescriptor targetType, String value) {

		Class<?> dimension = targetType.getResolvableType().getGeneric(0).resolve();
		if (dimension == null || dimension == Object.class || dimension == Quantity.class) {
			throw new IllegalArgumentException(
					"Cannot resolve the quantity dimension from %s for value '%s'; a raw Quantity is not supported"
							.formatted(targetType, value));
		}
		return dimension;
	}

	private static @Nullable Number parseNumber(String value) {

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {}

		try {
			return Long.parseLong(value);
		} catch (NumberFormatException ignored) {}

		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ignored) {}

		return null;
	}

}
