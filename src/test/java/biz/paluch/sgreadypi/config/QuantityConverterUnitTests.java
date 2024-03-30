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

import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Power;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QuantityConverter}.
 * 
 * @author Mark Paluch
 */
class QuantityConverterUnitTests {

	@Test
	@SuppressWarnings("unchecked")
	void shouldApplyConverter() {

		GenericConversionService service = new GenericConversionService();
		service.addConverter(new QuantityConverter());

		TypeDescriptor target = new TypeDescriptor(ResolvableType.forClassWithGenerics(Quantity.class, Power.class),
				Quantity.class, null);

		Quantity<Power> converted = (Quantity<Power>) service.convert("100 W", target);

		assertThat(converted.getUnit()).isEqualTo(Units.WATT);
		assertThat(converted.getValue().intValue()).isEqualTo(100);
	}

}
