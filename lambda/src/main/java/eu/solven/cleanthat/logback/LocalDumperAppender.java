package eu.solven.cleanthat.logback;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import cormoran.pepper.memory.IPepperMemoryConstants;

/**
 * This LogBack {@link Appender} shall help enabling a log to be produced for each run.
 * 
 * @author Benoit Lacelle
 *
 */
// TODO
public class LocalDumperAppender extends AppenderBase<ILoggingEvent> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalDumperAppender.class);

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
		StringBuilder sb = new StringBuilder();

		TL_LOGS.get().forEach(bytes -> sb.append(new String(bytes, StandardCharsets.UTF_8)));

		TL_LOGS.get().clear();

		String asString = sb.toString();
		LOGGER.debug("We cleared the log buffer after a flush ({} chars)", asString.length());
		return asString;
	}
}
