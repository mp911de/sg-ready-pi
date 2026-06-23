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

import static org.assertj.core.api.Assertions.*;

import biz.paluch.sgreadypi.SgReadyProperties;
import biz.paluch.sgreadypi.measure.Percent;
import biz.paluch.sgreadypi.measure.Watt;
import tech.units.indriya.unit.Units;

import java.util.List;
import java.util.Map;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;
import javax.measure.quantity.Temperature;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Unit tests for {@link QuantityConverter}.
 *
 * @author Mark Paluch
 */
class QuantityConverterUnitTests {

	QuantityConverter converter = new QuantityConverter();

	@Test
	void shouldConvertBareNumberToWatt() {
		assertThat(convert("100", Power.class)).isEqualTo(Watt.of(100));
	}

	@Test
	void shouldConvertValueWithExplicitUnit() {

		Quantity<?> converted = convert("100 W", Power.class);

		assertThat(converted.getUnit()).isEqualTo(Units.WATT);
		assertThat(converted.getValue().intValue()).isEqualTo(100);
	}

	@Test
	void shouldInterpretBareNumberAsPercentForDimensionless() {
		assertThat(convert("60", Dimensionless.class)).isEqualTo(Percent.of(60));
	}

	@Test
	void shouldHonorExplicitPercentUnit() {

		Quantity<?> converted = convert("65 %", Dimensionless.class);

		assertThat(converted.getUnit()).isEqualTo(Units.PERCENT);
		assertThat(converted.getValue().intValue()).isEqualTo(65);
	}

	@Test
	void shouldRejectUnitIncompatibleWithTargetDimension() {
		assertThatThrownBy(() -> convert("65 W", Dimensionless.class)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not compatible");
	}

	@Test
	void shouldFailFastOnUnsupportedDimension() {
		assertThatThrownBy(() -> convert("20", Temperature.class)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unsupported quantity dimension");
	}

	@Test
	void shouldFailFastOnRawQuantity() {

		TypeDescriptor raw = new TypeDescriptor(ResolvableType.forClass(Quantity.class), Quantity.class, null);

		assertThatThrownBy(() -> converter.convert("20", TypeDescriptor.valueOf(String.class), raw))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cannot resolve");
	}

	@Test // setter/field binding and record (constructor) binding share the same converter
	void shouldBindWattFieldsAndPercentRecordComponents() {

		Map<String, Object> source = Map.of( //
				"sg.heat-pump-power-consumption", "1000", //
				"sg.available-soc-off-margin", "5", //
				"sg.battery.pv-available", "40", //
				"sg.battery.pv-excess-on", "65", //
				"sg.battery.pv-excess-off", "60");

		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(converter);
		Binder binder = new Binder(List.of(new MapConfigurationPropertySource(source)), null, conversionService);

		SgReadyProperties properties = binder.bind("sg", SgReadyProperties.class).get();

		assertThat(properties.getHeatPumpPowerConsumption()).isEqualTo(Watt.of(1000));
		assertThat(properties.getAvailableSocOffMargin()).isEqualTo(Percent.of(5));
		// record components are bound through the constructor; the converter must still see Quantity<Dimensionless>
		assertThat(properties.getBattery().pvAvailable()).isEqualTo(Percent.of(40));
		assertThat(properties.getBattery().pvExcessOn()).isEqualTo(Percent.of(65));
		assertThat(properties.getBattery().pvExcessOff()).isEqualTo(Percent.of(60));
	}

	private Quantity<?> convert(String value, Class<?> dimension) {
		TypeDescriptor target = new TypeDescriptor(ResolvableType.forClassWithGenerics(Quantity.class, dimension),
				Quantity.class, null);
		return (Quantity<?>) converter.convert(value, TypeDescriptor.valueOf(String.class), target);
	}
}
