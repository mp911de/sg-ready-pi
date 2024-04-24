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
 * SG Ready Protocol state mapping.
 *
 * @param a
 * @param b
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

	@Override
	public String toString() {
		return "%s (%s:%s)".formatted(name(), a ? 1 : 0, b ? 1 : 0);
	}

	public SgReadyState valueOf(String value) {

		switch (value) {
			case "BLOCKED":
				return BLOCKED;
			case "NORMAL":
				return NORMAL;
			case "AVAILABLE_PV":
				return AVAILABLE_PV;
			case "EXCESS_PV":
				return EXCESS_PV;
		}

		throw new IllegalArgumentException("Cannot resolve " + value + " to a SgReadyState");

	}
}
