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
package biz.paluch.sgreadypi;

/**
 * SG Ready protocol state mapping the two relay signals to a named operating mode.
 *
 * @param name the operating mode name, for example {@code EXCESS_PV}.
 * @param a the SG Ready "A" signal.
 * @param b the SG Ready "B" signal.
 */
public record SgReadyState(String name, boolean a, boolean b) {

	/**
	 * Locked, reduce heating time up to 2 hrs for a day.
	 */
	public static final SgReadyState BLOCKED = new SgReadyState("BLOCKED", true, false);

	/**
	 * Normal operations.
	 */
	public static final SgReadyState NORMAL = new SgReadyState("NORMAL", false, false);

	/**
	 * Recommendation to turn heating on and increase temperature.
	 */
	public static final SgReadyState AVAILABLE_PV = new SgReadyState("AVAILABLE_PV", false, true);

	/**
	 * Signal to turn on heating. Can also increase temperature.
	 */
	public static final SgReadyState EXCESS_PV = new SgReadyState("EXCESS_PV", true, true);

	/**
	 * Create a state from the {@code a} and {@code b} flags.
	 *
	 * @param a SG Ready A state.
	 * @param b SG Ready B state.
	 * @return the SG Ready state.
	 */
	public static SgReadyState from(boolean a, boolean b) {
		if (a && b) {
			return EXCESS_PV;
		}
		if (a) {
			return BLOCKED;
		}
		if (b) {
			return AVAILABLE_PV;
		}
		return NORMAL;
	}

	public boolean isBlocked() {
		return this == BLOCKED;
	}

	public boolean isNormal() {
		return this == NORMAL;
	}

	public boolean isAvailablePv() {
		return this == AVAILABLE_PV;
	}

	public boolean isExcessPv() {
		return this == EXCESS_PV;
	}

	/**
	 * Resolve a state from its {@link #name() name}.
	 *
	 * @param value the operating mode name to resolve.
	 * @return the matching state.
	 * @throws IllegalArgumentException if {@code value} does not name a known state.
	 */
	public static SgReadyState valueOf(String value) {
		return switch (value) {
			case "BLOCKED" -> BLOCKED;
			case "NORMAL" -> NORMAL;
			case "AVAILABLE_PV" -> AVAILABLE_PV;
			case "EXCESS_PV" -> EXCESS_PV;
			default -> throw new IllegalArgumentException("Cannot resolve " + value + " to a SgReadyState");
		};

	}

	@Override
	public String toString() {
		return "%s (%s:%s)".formatted(name(), a ? 1 : 0, b ? 1 : 0);
	}
}
