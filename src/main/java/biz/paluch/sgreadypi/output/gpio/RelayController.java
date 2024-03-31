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
package biz.paluch.sgreadypi.output.gpio;

import biz.paluch.sgreadypi.output.ConditionalOnRaspberryPi;
import lombok.Value;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.pi4j.io.gpio.digital.DigitalState;

/**
 * @author Mark Paluch
 */
@RestController
@ConditionalOnRaspberryPi
@RequestMapping("relay")
@Value
class RelayController {

	PiRelHat3Ch relay;

	@GetMapping("{channel}")
	public String getState(@PathVariable("channel") int channel) {

		return switch (channel) {
			case 1 -> relay.getCh1().state().name();
			case 2 -> relay.getCh2().state().name();
			default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		};
	}

	@PostMapping("{channel}")
	public String getState(@PathVariable("channel") int channel, @RequestBody String body) {

		DigitalState state = DigitalState.valueOf(body);

		return switch (channel) {
			case 1 -> relay.getCh1().state(state).state().name();
			case 2 -> relay.getCh2().state(state).state().name();
			default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		};
	}

}
