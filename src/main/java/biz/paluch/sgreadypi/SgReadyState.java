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
public record SgReadyState(boolean a, boolean b) {

	/**
	 * Locked, reduce heating time up to 2 hrs for a day.
	 */
	public static final SgReadyState BLOCKED = new SgReadyState(true, false);

	/**
	 * Normal operations.
	 */
	public static final SgReadyState NORMAL = new SgReadyState(false, false);

	/**
	 * Recommendation to turn heating on and increase temperature.
	 */
	public static final SgReadyState AVAILABLE_PV = new SgReadyState(false, true);

	/**
	 * Signal to turn on heating. Can also increase temperature.
	 */
	public static final SgReadyState EXCESS_PV = new SgReadyState(true, true);

	@Override
	public String toString() {
		return "(%s:%s)".formatted(a ? 1 : 0, b ? 1 : 0);
	}
}
