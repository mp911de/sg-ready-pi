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
package biz.paluch.sgreadypi.output;

import biz.paluch.sgreadypi.SgReadyState;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.TaskScheduler;

/**
 * Debouncing {@link SgReadyStateConsumer} to avoid state flickering and reduce wear on the output.
 * 
 * @author Mark Paluch
 */
@Slf4j
public class DebounceStateConsumer implements SgReadyStateConsumer {

	private final SgReadyStateConsumer delegate;

	private final TaskScheduler scheduler;

	private final Duration debounce;

	private final AtomicBoolean debounceActive = new AtomicBoolean();

	private final Clock clock = Clock.systemDefaultZone();

	volatile SgReadyState current = SgReadyState.NORMAL;
	volatile SgReadyState next = SgReadyState.NORMAL;

	volatile Instant lastUpdate = Instant.MIN;

	public DebounceStateConsumer(SgReadyStateConsumer delegate, TaskScheduler scheduler, Duration debounce) {
		this.delegate = delegate;
		this.scheduler = scheduler;
		this.debounce = debounce;
	}

	@Override
	public void onState(SgReadyState state) {

		Instant now = clock.instant();
		Instant nextUpdate = getNextUpdate();
		next = state;

		if (current.equals(state)) {
			return;
		}

		if (now.isAfter(nextUpdate)) {
			doUpdate(state);
		} else {

			if (debounceActive.compareAndSet(false, true)) {
				log.info("Debounce until {} to set new state {}", Duration.between(now, nextUpdate), state);
				scheduler.schedule(() -> {

					debounceActive.set(false);

					SgReadyState nextState = next;
					if (nextState != null && !nextState.equals(current)) {
						log.info("Applying debounced state {}", nextState);
						doUpdate(nextState);
					} else {
						log.info("Skipping debounced state {}", nextState);
					}
				}, nextUpdate);
			} else {
				log.info("Update debounce until {} to set new state {}", Duration.between(now, nextUpdate), state);
			}
		}
	}

	private void doUpdate(SgReadyState nextState) {
		lastUpdate = clock.instant();
		current = nextState;
		delegate.onState(nextState);
	}

	boolean isSynchronized() {
		return current.equals(next);
	}

	Instant getNextUpdate() {
		return lastUpdate.plus(debounce);
	}

	SgReadyState getCurrent() {
		return current;
	}

	SgReadyState getNext() {
		return next;
	}

	Instant getLastUpdate() {
		return lastUpdate;
	}

}
