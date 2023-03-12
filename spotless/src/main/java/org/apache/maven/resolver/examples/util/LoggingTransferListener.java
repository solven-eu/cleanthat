/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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
package org.apache.maven.resolver.examples.util;

import static java.util.Objects.requireNonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simplistic transfer listener that logs uploads/downloads to the console.
 * 
 * @author https://github.com/apache/maven-resolver/
 */
@SuppressWarnings({ "PMD", "checkstyle:JavadocType", "checkstyle:AvoidInlineConditionals", "checkstyle:MagicNumber" })
public class LoggingTransferListener extends AbstractTransferListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingTransferListener.class);

	private final Map<TransferResource, Long> downloads = new ConcurrentHashMap<>();

	private int lastLength;

	@Override
	public void transferInitiated(TransferEvent event) {
		requireNonNull(event, "event cannot be null");
		String message;
		if (event.getRequestType() == TransferEvent.RequestType.PUT) {
			message = "Uploading";
		} else {
			message = "Downloading";
		}

		LOGGER.info(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
	}

	@Override
	public void transferProgressed(TransferEvent event) {
		requireNonNull(event, "event cannot be null");
		TransferResource resource = event.getResource();
		downloads.put(resource, event.getTransferredBytes());

		var buffer = new StringBuilder(64);

		for (Map.Entry<TransferResource, Long> entry : downloads.entrySet()) {
			long total = entry.getKey().getContentLength();
			long complete = entry.getValue();

			buffer.append(getStatus(complete, total)).append("  ");
		}

		var pad = lastLength - buffer.length();
		lastLength = buffer.length();
		pad(buffer, pad);
		buffer.append('\r');

		LOGGER.info("{}", buffer);
	}

	private String getStatus(long complete, long total) {
		if (total >= 1_024) {
			return toKB(complete) + "/" + toKB(total) + " KB ";
		} else if (total >= 0) {
			return complete + "/" + total + " B ";
		} else if (complete >= 1_024) {
			return toKB(complete) + " KB ";
		} else {
			return complete + " B ";
		}
	}

	private void pad(StringBuilder buffer, int spaces) {
		var block = "                                        ";
		while (spaces > 0) {
			var n = Math.min(spaces, block.length());
			buffer.append(block, 0, n);
			spaces -= n;
		}
	}

	@Override
	public void transferSucceeded(TransferEvent event) {
		requireNonNull(event, "event cannot be null");
		transferCompleted(event);

		TransferResource resource = event.getResource();
		long contentLength = event.getTransferredBytes();
		if (contentLength >= 0) {
			var type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
			String len;
			if (contentLength >= 1_024) {
				len = toKB(contentLength) + " KB";
			} else {
				len = contentLength + " B";
			}

			var throughput = "";
			long duration = System.currentTimeMillis() - resource.getTransferStartTime();
			if (duration > 0) {
				long bytes = contentLength - resource.getResumeOffset();
				var format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
				var kbPerSec = (bytes / 1_024.0) / (duration / 1_000.0);
				throughput = " at " + format.format(kbPerSec) + " KB/sec";
			}

			LOGGER.info(type + ": "
					+ resource.getRepositoryUrl()
					+ resource.getResourceName()
					+ " ("
					+ len
					+ throughput
					+ ")");
		}
	}

	@Override
	public void transferFailed(TransferEvent event) {
		requireNonNull(event, "event cannot be null");
		transferCompleted(event);

		if (!(event.getException() instanceof MetadataNotFoundException)) {
			LOGGER.warn("transferFailed", new RuntimeException("Need the stack", event.getException()));
		}
	}

	private void transferCompleted(TransferEvent event) {
		requireNonNull(event, "event cannot be null");
		downloads.remove(event.getResource());

		var buffer = new StringBuilder(64);
		pad(buffer, lastLength);
		buffer.append('\r');
		LOGGER.info("{}", buffer);
	}

	public void transferCorrupted(TransferEvent event) {
		requireNonNull(event, "event cannot be null");
		LOGGER.warn("transferCorrupted", new RuntimeException("Need the stack", event.getException()));
	}

	@SuppressWarnings("checkstyle:magicnumber")
	protected long toKB(long bytes) {
		return (bytes + 1_023) / 1_024;
	}

}
