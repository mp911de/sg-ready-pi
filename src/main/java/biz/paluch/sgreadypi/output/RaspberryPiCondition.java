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

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import com.pi4j.boardinfo.definition.BoardModel;
import com.pi4j.boardinfo.model.DetectedBoard;
import com.pi4j.boardinfo.util.BoardModelDetection;

/**
 * Condition implementation for RaspberryPi platform detection.
 * 
 * @author Mark Paluch
 */
class RaspberryPiCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {

		DetectedBoard detectedBoard = BoardModelDetection.getDetectedBoard();

		if (detectedBoard.getBoardModel() == BoardModel.UNKNOWN) {
			return ConditionOutcome.noMatch("Unknown board");
		}

		return ConditionOutcome.match("Running on %s".formatted(detectedBoard));
	}
}
