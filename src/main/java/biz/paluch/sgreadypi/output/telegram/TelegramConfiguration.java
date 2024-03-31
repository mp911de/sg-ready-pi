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
package biz.paluch.sgreadypi.output.telegram;

import biz.paluch.sgreadypi.PowerGeneratorService;
import biz.paluch.sgreadypi.PowerMeter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Telegram configuration.
 * 
 * @author Mark Paluch
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TelegramProperties.class)
@ConditionalOnProperty("sg.telegram.token")
public class TelegramConfiguration {

	@Bean
	TelegramService telegramService(TelegramProperties properties, PowerGeneratorService powerGeneratorService,
			PowerMeter powerMeter) {
		return new TelegramService(properties.getToken(), properties.getChatId(), powerGeneratorService, powerMeter,
				properties.getLocale());
	}
}
