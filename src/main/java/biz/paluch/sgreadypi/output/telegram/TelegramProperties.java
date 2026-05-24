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

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Telegram notifications.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties(prefix = "sg.telegram")
@SuppressWarnings("NullAway.Init") // fields are populated by Spring configuration property binding
public class TelegramProperties {

	private String token;

	private long chatId;

	private Locale locale = Locale.getDefault();

	public TelegramProperties() {}

	public String getToken() {
		return this.token;
	}

	public long getChatId() {
		return this.chatId;
	}

	public Locale getLocale() {
		return this.locale;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setChatId(long chatId) {
		this.chatId = chatId;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public String toString() {
		return "TelegramProperties(token=" + this.getToken() + ", chatId=" + this.getChatId() + ", locale="
				+ this.getLocale() + ")";
	}
}
