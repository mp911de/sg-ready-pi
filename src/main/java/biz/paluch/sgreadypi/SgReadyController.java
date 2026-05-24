/*
 * Copyright 2026 the original author or authors.
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
package biz.paluch.sgreadypi;

import biz.paluch.sgreadypi.output.gpio.Relay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing and overriding the current SG Ready state.
 *
 * @author Mark Paluch
 */
@RestController
@RequestMapping("api/sg-ready")
public class SgReadyController {

	private final SgReadyControlLoop controller;

	private final Relay relay;

	public SgReadyController(SgReadyControlLoop controller, Relay relay) {
		this.controller = controller;
		this.relay = relay;
	}

	@GetMapping
	public Map<String, Object> getState() {

		Map<String, Object> state = new LinkedHashMap<>();
		SgReadyState sgReady = relay.getState();

		state.put("state", sgReady.name());
		state.put("a", sgReady.a());
		state.put("b", sgReady.b());

		SgReadyControlLoop.Decision decision = controller.getDecision();
		List<String> conditions = new ArrayList<>();

		if (decision != null) {
			SgReadyControlLoop.ConditionOutcome outcome = decision.conditionOutcome();
			while (outcome != null) {
				conditions.addFirst(((outcome.isMatch() ? "Did match: " : "Did not match: ") + outcome.getMessage()));
				outcome = outcome.getParent();
			}
		}

		state.put("decision", conditions);
		return state;
	}

	@PostMapping
	public String set(@RequestBody String body) {
		if (StringUtils.hasText(body)) {
			SgReadyState sgReadyState = SgReadyState.valueOf(body.trim());
			relay.setState(sgReadyState);
			return sgReadyState.name();
		}
		return "NOT_APPLIED";
	}

	@PostMapping("a")
	public void setA(@RequestBody String body) {
		if (StringUtils.hasText(body)) {
			relay.setState(SgReadyState.from(Boolean.getBoolean(body), relay.getState().b()));
		}
	}

	@PostMapping("b")
	public void setB(@RequestBody String body) {
		if (StringUtils.hasText(body)) {
			relay.setState(SgReadyState.from(relay.getState().a(), Boolean.getBoolean(body)));
		}
	}

	public SgReadyControlLoop getController() {
		return this.controller;
	}

	public Relay getRelay() {
		return this.relay;
	}

}
