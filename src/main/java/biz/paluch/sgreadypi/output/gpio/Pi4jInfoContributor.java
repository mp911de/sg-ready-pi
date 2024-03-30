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

import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import com.pi4j.boardinfo.model.DetectedBoard;
import com.pi4j.boardinfo.util.BoardModelDetection;
import com.pi4j.context.Context;
import com.pi4j.io.IO;
import com.pi4j.io.IOType;

/**
 * @author Mark Paluch
 */
@Component
public class Pi4jInfoContributor implements InfoContributor {

	private final Context context;
	private final DetectedBoard detectedBoard;

	public Pi4jInfoContributor(Context context) {
		this.context = context;
		this.detectedBoard = BoardModelDetection.getDetectedBoard();
	}

	@Override
	public void contribute(Info.Builder builder) {

		builder.withDetail("os", detectedBoard.getOperatingSystem());
		builder.withDetail("board", detectedBoard.getBoardModel());
		builder.withDetail("java", detectedBoard.getJavaInfo());

		try {
			builder.withDetail("pi4jRegistry", context.registry().all());
		} catch (Exception ignore) {}
	}

}
