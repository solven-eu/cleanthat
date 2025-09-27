/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.logback;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import eu.solven.pepper.memory.IPepperMemoryConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * This LogBack {@link Appender} shall help enabling a log to be produced for each run.
 * 
 * @author Benoit Lacelle
 *
 */
// TODO
@Slf4j
public class LocalDumperAppender extends AppenderBase<ILoggingEvent> {

	private static final ThreadLocal<List<byte[]>> TL_LOGS = new ThreadLocal<>() {
		@Override
		protected List<byte[]> initialValue() {
			// Collections.synchronizedList is not necessary as AppenderBase is ThreadSafe
			return new ArrayList<>();
		}
	};
	/**
	 * It is the encoder which is ultimately responsible for writing the event to an {@link OutputStream}.
	 */
	protected Encoder<ILoggingEvent> encoder;

	public void setEncoder(Encoder<ILoggingEvent> encoder) {
		this.encoder = encoder;
	}

	@Override
	protected void append(ILoggingEvent event) {
		byte[] byteArray = this.encoder.encode(event);
		TL_LOGS.get().add(byteArray);

		if (TL_LOGS.get().size() >= IPepperMemoryConstants.KB_INT) {
			LOGGER.warn("We cleared the log buffer as grown too big");
			TL_LOGS.get().clear();
			TL_LOGS.get().add("We encountered too many logs".getBytes(StandardCharsets.UTF_8));
		}
	}

	public String flushLogs() {
		var sb = new StringBuilder();

		TL_LOGS.get().forEach(bytes -> sb.append(new String(bytes, StandardCharsets.UTF_8)));

		TL_LOGS.get().clear();

		var asString = sb.toString();
		LOGGER.debug("We cleared the log buffer after a flush ({} chars)", asString.length());
		return asString;
	}
}
