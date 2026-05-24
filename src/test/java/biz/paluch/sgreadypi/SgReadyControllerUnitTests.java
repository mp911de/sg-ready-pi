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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import biz.paluch.sgreadypi.output.gpio.Relay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link SgReadyController}.
 *
 * @author Mark Paluch
 */
class SgReadyControllerUnitTests {

	TestRelay relay;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		relay = new TestRelay();
		mockMvc = MockMvcBuilders.standaloneSetup(new SgReadyController(null, relay)).build();
	}

	@Test
	void shouldSetSignalAToTrue() throws Exception {

		relay.state = SgReadyState.NORMAL;

		mockMvc.perform(post("/api/sg-ready/a").contentType(MediaType.TEXT_PLAIN).content("true"))
				.andExpect(status().isOk());

		assertThat(relay.state).isEqualTo(SgReadyState.BLOCKED);
	}

	@Test
	void shouldSetSignalBToFalse() throws Exception {

		relay.state = SgReadyState.EXCESS_PV;

		mockMvc.perform(post("/api/sg-ready/b").contentType(MediaType.TEXT_PLAIN).content("false"))
				.andExpect(status().isOk());

		assertThat(relay.state).isEqualTo(SgReadyState.BLOCKED);
	}

	@Test
	void shouldTrimBooleanRequestBody() throws Exception {

		relay.state = SgReadyState.NORMAL;

		mockMvc.perform(post("/api/sg-ready/b").contentType(MediaType.TEXT_PLAIN).content(" true \n"))
				.andExpect(status().isOk());

		assertThat(relay.state).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldRejectInvalidBooleanRequestBody() throws Exception {

		mockMvc.perform(post("/api/sg-ready/a").contentType(MediaType.TEXT_PLAIN).content("yes"))
				.andExpect(status().isBadRequest());

		assertThat(relay.setCalls).isZero();
	}

	static class TestRelay implements Relay {

		SgReadyState state = SgReadyState.NORMAL;

		int setCalls;

		@Override
		public void onState(SgReadyState state) {
			setState(state);
		}

		@Override
		public SgReadyState getState() {
			return state;
		}

		@Override
		public void setState(SgReadyState state) {
			this.state = state;
			this.setCalls++;
		}

	}

}
