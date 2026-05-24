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
import biz.paluch.sgreadypi.SgReadyState;
import biz.paluch.sgreadypi.output.SgReadyStateConsumer;
import jakarta.annotation.PreDestroy;

import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.slf4j.Logger;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

/**
 * {@link SgReadyStateConsumer} sending Telegram notifications to a specified chatId.
 *
 * @author Mark Paluch
 */
public class TelegramService implements SgReadyStateConsumer {

	private static final Map<SgReadyState, String> ICONS = Map.of(SgReadyState.BLOCKED, "🛑",
			SgReadyState.NORMAL, "🔌",
			SgReadyState.AVAILABLE_PV, "⚡️",
			SgReadyState.EXCESS_PV, "🔋");
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(TelegramService.class);

	private final TelegramBot bot;
	private final long chatId;

	private final PowerGeneratorService inverters;
	private final PowerMeter powerMeter;
	private final ResourceBundle resourceBundle;
	private final Locale locale;

	public TelegramService(String token, long chatId, PowerGeneratorService inverters, PowerMeter powerMeter,
			Locale locale) {
		this.bot = new TelegramBot(token);
		this.chatId = chatId;
		this.inverters = inverters;
		this.powerMeter = powerMeter;
		this.resourceBundle = ResourceBundle.getBundle("telegram", locale);
		this.locale = locale;
	}

	@PreDestroy
	public void preDestroy() {
		bot.shutdown();
	}

	@Override
	public void onState(SgReadyState state) {

		Quantity<Power> generatorPower = inverters.getGeneratorPower().getAverage();
		Quantity<Power> ingress = powerMeter.getIngress().getAverage();
		Quantity<Dimensionless> soc = inverters.getBatteryStateOfCharge();

		String stateName = resourceBundle.getString("state." + state.name());
		String messageTemplate = resourceBundle.getString("message");
		Formatter formatter = new Formatter(this.locale);
		String message = formatter.format(messageTemplate, ICONS.get(state), stateName, ingress, generatorPower, soc)
				.toString();

		SendMessage sendMessage = new SendMessage(chatId, message);
		sendMessage.parseMode(ParseMode.HTML);

		try {
			SendResponse response = bot.execute(sendMessage);
			if (!response.isOk()) {
				log.warn("Cannot send Telegram message: %s".formatted(response.description()));
			}
		} catch (Exception e) {
			log.warn("Cannot send Telegram message", e);
		}
	}
}
