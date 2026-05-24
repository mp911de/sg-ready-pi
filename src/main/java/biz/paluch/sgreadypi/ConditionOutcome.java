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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Outcome for a condition match, including log message. Outcomes nest to form an explanation trail behind a
 * {@link Decision}.
 *
 * @author Mark Paluch
 */
public class ConditionOutcome {

	private final @Nullable ConditionOutcome parent;

	private final boolean match;

	private final String message;

	private ConditionOutcome(@Nullable ConditionOutcome parent, boolean match, String message) {
		this.parent = parent;
		this.match = match;
		this.message = message;
	}

	/**
	 * Create a {@link ConditionOutcome} for a matching condition including a {@code message}.
	 *
	 * @param message
	 * @return
	 */
	public static ConditionOutcome match(String message) {
		return new ConditionOutcome(null, true, message);
	}

	/**
	 * Create a {@link ConditionOutcome} for a not matching condition including a {@code message}.
	 *
	 * @param message
	 * @return
	 */
	public static ConditionOutcome noMatch(String message) {
		return new ConditionOutcome(null, false, message);
	}

	/**
	 * Create a nested {@link ConditionOutcome} for a matching condition including a {@code message}.
	 *
	 * @param message
	 * @return
	 */
	public ConditionOutcome nestedMatch(String message) {
		return new ConditionOutcome(this, true, message);
	}

	/**
	 * Create a nested {@link ConditionOutcome} for a not matching condition including a {@code message}.
	 *
	 * @param message
	 * @return
	 */
	public ConditionOutcome nestedNoMatch(String message) {
		return new ConditionOutcome(this, false, message);
	}

	/**
	 * Create a nested {@link ConditionOutcome} for a nested {@link ConditionOutcome}.
	 *
	 * @param message
	 * @return
	 */
	public ConditionOutcome nested(ConditionOutcome nested) {
		return new ConditionOutcome(this, nested.isMatch(), nested.message);
	}

	@Nullable
	public ConditionOutcome getParent() {
		return parent;
	}

	public boolean isMatch() {
		return match;
	}

	public String getMessage() {
		return message;
	}

	/**
	 * Render the explanation trail as an ordered list, from the root outcome down to this one. Each entry is prefixed
	 * with whether the step matched.
	 *
	 * @return the ordered, human-readable reasoning trail.
	 */
	public List<String> explain() {

		List<String> trail = new ArrayList<>();

		for (ConditionOutcome outcome = this; outcome != null; outcome = outcome.getParent()) {
			trail.add(0, (outcome.isMatch() ? "Did match: " : "Did not match: ") + outcome.getMessage());
		}

		return trail;
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		if (parent != null) {
			builder.append(parent).append(" -> ");
		}

		builder.append(isMatch() ? "Match" : "No match").append(": ").append(getMessage());

		return builder.toString();
	}
}
