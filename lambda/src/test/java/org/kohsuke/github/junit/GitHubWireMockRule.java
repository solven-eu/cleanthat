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
package org.kohsuke.github.junit;

import static com.github.tomakehurst.wiremock.client.WireMock.proxyAllTo;
import static com.github.tomakehurst.wiremock.client.WireMock.recordSpec;
import static com.github.tomakehurst.wiremock.common.Gzip.unGzipToString;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

// TODO: Auto-generated Javadoc
/**
 * The standard WireMockRule eagerly initializes a WireMockServer. This version suptakes a laze approach allowing us to
 * automatically isolate snapshots for each method.
 *
 * @author Liam Newman
 */
// https://github.com/hub4j/github-api/blob/main/src/test/java/org/kohsuke/github/junit/GitHubWireMockRule.java
public class GitHubWireMockRule extends WireMockMultiServerRule {
	private static final Logger LOGGER = LoggerFactory.getLogger(GitHubWireMockRule.class);

	public static final String KEY_TAKE_SNAPSHOT = "test.github.takeSnapshot";
	// By default the wiremock tests will run without proxy or taking a snapshot.
	// The tests will use only the stubbed data and will fail if requests are made for missing data.
	// You can use the proxy without taking a snapshot while writing and debugging tests.
	// You cannot take a snapshot without proxying.
	private static final boolean takeSnapshot = Boolean.getBoolean(KEY_TAKE_SNAPSHOT);
	private static final boolean useProxy =
			takeSnapshot || System.getProperty("test.github.useProxy", "false") != "false";

	/**
	 * Customize record spec.
	 *
	 * @param customizeRecordSpec
	 *            the customize record spec
	 */
	public void customizeRecordSpec(Consumer<RecordSpecBuilder> customizeRecordSpec) {
		this.customizeRecordSpec = customizeRecordSpec;
	}

	private Consumer<RecordSpecBuilder> customizeRecordSpec = null;

	/**
	 * Instantiates a new git hub wire mock rule.
	 */
	public GitHubWireMockRule() {
		this(WireMockConfiguration.options());
	}

	/**
	 * Instantiates a new git hub wire mock rule.
	 *
	 * @param options
	 *            the options
	 */
	public GitHubWireMockRule(WireMockConfiguration options) {
		this(options, true);
	}

	/**
	 * Instantiates a new git hub wire mock rule.
	 *
	 * @param options
	 *            the options
	 * @param failOnUnmatchedRequests
	 *            the fail on unmatched requests
	 */
	public GitHubWireMockRule(WireMockConfiguration options, boolean failOnUnmatchedRequests) {
		super(options, failOnUnmatchedRequests);
	}

	/**
	 * Api server.
	 *
	 * @return the wire mock server
	 */
	public WireMockServer apiServer() {
		return servers.get("default");
	}

	/**
	 * Raw server.
	 *
	 * @return the wire mock server
	 */
	public WireMockServer rawServer() {
		return servers.get("raw");
	}

	/**
	 * Uploads server.
	 *
	 * @return the wire mock server
	 */
	public WireMockServer uploadsServer() {
		return servers.get("uploads");
	}

	/**
	 * Codeload server.
	 *
	 * @return the wire mock server
	 */
	public WireMockServer codeloadServer() {
		return servers.get("codeload");
	}

	/**
	 * Actions user content server.
	 *
	 * @return the wire mock server
	 */
	public WireMockServer actionsUserContentServer() {
		return servers.get("actions-user-content");
	}

	/**
	 * Checks if is use proxy.
	 *
	 * @return true, if is use proxy
	 */
	public boolean isUseProxy() {
		return GitHubWireMockRule.useProxy;
	}

	/**
	 * Checks if is take snapshot.
	 *
	 * @return true, if is take snapshot
	 */
	public boolean isTakeSnapshot() {
		return GitHubWireMockRule.takeSnapshot;
	}

	/**
	 * Initialize servers.
	 */
	@Override
	protected void initializeServers() {
		super.initializeServers();
		initializeServer("default", new GitHubApiResponseTransformer(this));

		// only start non-api servers if we might need them
		mayInitializeServer("raw");
		mayInitializeServer("uploads");
		mayInitializeServer("codeload");
		mayInitializeServer("actions-user-content");
	}

	private void mayInitializeServer(String serverName) {
		boolean folderExists = Files.exists(fileToServerRecords(serverName));
		if (folderExists || isUseProxy()) {
			if (folderExists && isUseProxy()) {
				LOGGER.warn("Beware the proxy for `{}` will take advantage of existing stubs", serverName);
			}

			initializeServer(serverName);
		}
	}

	private Path fileToServerRecords(String serverName) {
		WireMockServer apiServer = apiServer();
		return fileToServerRecords(serverName, apiServer);
	}

	public static Path fileToServerRecords(String serverName, WireMockServer apiServer) {
		String serverFileRoot = apiServer.getOptions().filesRoot().getPath();
		return Paths.get(serverFileRoot + WireMockMultiServerRule.getDirectorySuffix(serverName));
	}

	/**
	 * Before.
	 */
	@Override
	protected void before() {
		super.before();
		if (isUseProxy()) {
			this.apiServer().stubFor(proxyAllTo("https://api.github.com").atPriority(100));

			if (this.uploadsServer() != null) {
				this.uploadsServer().stubFor(proxyAllTo("https://uploads.github.com").atPriority(100));
			}
			if (this.rawServer() != null) {
				this.rawServer().stubFor(proxyAllTo("https://raw.githubusercontent.com").atPriority(100));
			}
			if (this.codeloadServer() != null) {
				this.codeloadServer().stubFor(proxyAllTo("https://codeload.github.com").atPriority(100));
			}
			if (this.actionsUserContentServer() != null) {
				this.actionsUserContentServer()
						.stubFor(proxyAllTo("https://pipelines.actions.githubusercontent.com").atPriority(100));
			}
		}
	}

	/**
	 * After.
	 */
	@Override
	protected void after() {
		super.after();
		if (isTakeSnapshot()) {
			recordSnapshot(this.apiServer(), "https://api.github.com", false);
			recordSnapshot(this.uploadsServer(), "https://uploads.github.com", false);

			// For raw server, only fix up mapping files
			recordSnapshot(this.rawServer(), "https://raw.githubusercontent.com", true);
			recordSnapshot(this.codeloadServer(), "https://codeload.github.com", true);
			recordSnapshot(this.actionsUserContentServer(), "https://pipelines.actions.githubusercontent.com", true);
		}
	}

	private static String targetToId(String target) {
		switch (target) {
		case "https://api.github.com":
			return "default";
		case "https://uploads.github.com":
			return "uploads";
		case "https://raw.githubusercontent.com":
			return "raw";
		case "https://codeload.github.com":
			return "codeload";
		case "https://pipelines.actions.githubusercontent.com":
			return "actions-user-content";

		default:
			throw new IllegalArgumentException("Invalid target: " + target);
		}
	}

	private void recordSnapshot(WireMockServer server, String target, boolean isRawServer) {
		if (server == null) {
			LOGGER.info("There is no server recording {}", target);
			return;
		}

		final RecordSpecBuilder recordSpecBuilder = recordSpec().forTarget(target)
				// "If-None-Match" header used for ETag matching for caching connections
				.captureHeader("If-None-Match")
				// "If-Modified-Since" header used for ETag matching for caching connections
				.captureHeader("If-Modified-Since")
				.captureHeader("Cache-Control")
				// "Accept" header is used to specify previews. If it changes expected data may not be retrieved.
				.captureHeader("Accept")
				// This is required, or some requests will return data from unexpected stubs
				// For example, if you update "title" and "body", and then update just "title" to the same value
				// the mock framework will treat those two requests as equivalent, which we do not want.
				.chooseBodyMatchTypeAutomatically(true, false, false)
				.extractTextBodiesOver(255);

		if (customizeRecordSpec != null) {
			customizeRecordSpec.accept(recordSpecBuilder);
		}

		server.snapshotRecord(recordSpecBuilder);

		// After taking the snapshot, format the output
		formatTestResources(fileToServerRecords(targetToId(target)), isRawServer);
	}

	/**
	 * Gets the request count.
	 *
	 * @return the request count
	 */
	public int getRequestCount() {
		return getRequestCount(apiServer());
	}

	/**
	 * Gets the request count.
	 *
	 * @param server
	 *            the server
	 * @return the request count
	 */
	public static int getRequestCount(WireMockServer server) {
		return server.countRequestsMatching(RequestPatternBuilder.allRequests().build()).getCount();
	}

	private void formatTestResources(Path path, boolean isRawServer) {
		// The more consistent we can make the json output the more meaningful it will be.
		Gson g = new Gson().newBuilder()
				.serializeNulls()
				.disableHtmlEscaping()
				.setPrettyPrinting()
				.registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
					@Override
					public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
						// Gson by default output numbers as doubles - 0.0
						// Remove the tailing .0, as most most numbers are integer value
						if (src == src.longValue())
							return new JsonPrimitive(src.longValue());
						return new JsonPrimitive(src);
					}
				})
				.create();

		try {
			Map<String, String> idToIndex = new HashMap<>();

			// Match all the ids to request indexes
			Files.walk(path).forEach(filePath -> {
				try {
					if (filePath.toString().endsWith(".json")
							&& filePath.toString().contains("/" + KEY_MAPPINGS + "/")) {
						var fileText = new String(Files.readAllBytes(filePath));
						Object parsedObject = g.fromJson(fileText, Object.class);
						addMappingId((Map<String, Object>) parsedObject, idToIndex);
					}
				} catch (Exception e) {
					throw new RuntimeException("Files could not be read: " + filePath.toString(), e);
				}
			});

			// Update all
			Files.walk(path).forEach(filePath -> {
				try {
					Map.Entry<String, String> entry = getId(filePath, idToIndex);
					if (entry != null) {
						filePath = renameFileToIndex(filePath, entry);
					}
					// For raw server, only fix up mapping files
					if (isRawServer && !filePath.toString().contains(KEY_MAPPINGS)) {
						return;
					}
					if (filePath.toString().endsWith(".json")) {
						var fileText = new String(Files.readAllBytes(filePath));
						// while recording responses we replaced all github calls localhost
						// now we reverse that for storage.
						fileText = fileText.replace(apiServer().baseUrl(), "https://api.github.com");

						if (this.rawServer() != null) {
							fileText = fileText.replace(rawServer().baseUrl(), "https://raw.githubusercontent.com");
						}

						if (this.uploadsServer() != null) {
							fileText = fileText.replace(uploadsServer().baseUrl(), "https://uploads.github.com");
						}

						if (this.codeloadServer() != null) {
							fileText = fileText.replace(codeloadServer().baseUrl(), "https://codeload.github.com");
						}

						if (this.actionsUserContentServer() != null) {
							fileText = fileText.replace(actionsUserContentServer().baseUrl(),
									"https://pipelines.actions.githubusercontent.com");
						}

						// point bodyFile in the mapping to the renamed body file
						if (entry != null && filePath.toString().contains(KEY_MAPPINGS)) {
							fileText = fileText.replace("-" + entry.getKey(), "-" + entry.getValue());
						}

						// Can be Array or Map
						Object parsedObject = g.fromJson(fileText, Object.class);
						fileText = g.toJson(parsedObject);
						Files.write(filePath, fileText.getBytes());
					}
				} catch (Exception e) {
					throw new RuntimeException("Files could not be written: " + filePath.toString(), e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Files could not be written", e);
		}
	}

	private void addMappingId(Map<String, Object> parsedObject, Map<String, String> idToIndex) {
		var id = (String) parsedObject.getOrDefault("id", null);
		var insertionIndex = ((Double) parsedObject.getOrDefault("insertionIndex", 0.0)).longValue();
		if (id != null && insertionIndex > 0) {
			idToIndex.put(id, Long.toString(insertionIndex));
		}
	}

	private Map.Entry<String, String> getId(Path filePath, Map<String, String> idToIndex) throws IOException {
		var filePathString = filePath.toString();
		for (Map.Entry<String, String> item : idToIndex.entrySet()) {
			if (filePathString.contains(item.getKey())) {
				return item;
			}
		}
		return null;
	}

	private Path renameFileToIndex(Path filePath, Map.Entry<String, String> idToIndex) throws IOException {
		var filePathString = filePath.toString();
		var targetPath = new File(filePathString.replace(idToIndex.getKey(), idToIndex.getValue())).toPath();
		Files.move(filePath, targetPath);

		return targetPath;
	}

	/**
	 * Map to mock git hub.
	 *
	 * @param body
	 *            the body
	 * @return the string
	 */
	@Nonnull
	public String mapToMockGitHub(String body) {
		body = body.replace("https://api.github.com", this.apiServer().baseUrl());

		body = replaceTargetUrl(body, rawServer(), "https://raw.githubusercontent.com", "/raw");
		body = replaceTargetUrl(body, uploadsServer(), "https://uploads.github.com", "/uploads");
		body = replaceTargetUrl(body, codeloadServer(), "https://codeload.github.com", "/codeload");
		body = replaceTargetUrl(body,
				actionsUserContentServer(),
				"https://pipelines.actions.githubusercontent.com",
				"/actions-user-content");
		return body;
	}

	@Nonnull
	private String replaceTargetUrl(String body,
			WireMockServer wireMockServer,
			String rawTarget,
			String inactiveTarget) {
		if (wireMockServer != null) {
			body = body.replace(rawTarget, wireMockServer.baseUrl());
		} else {
			body = body.replace(rawTarget, this.apiServer().baseUrl() + inactiveTarget);
		}
		return body;
	}

	/**
	 * A number of modifications are needed as runtime to make responses target the WireMock server and not accidentally
	 * switch to using the live github servers.
	 */
	private static class GitHubApiResponseTransformer extends ResponseTransformer {
		private final GitHubWireMockRule rule;

		public GitHubApiResponseTransformer(GitHubWireMockRule rule) {
			this.rule = rule;
		}

		@Override
		public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
			Response.Builder builder = Response.Builder.like(response);
			Collection<HttpHeader> headers = response.getHeaders().all();

			fixListTraversalHeader(response, headers);
			fixLocationHeader(response, headers);

			if ("application/json".equals(response.getHeaders().getContentTypeHeader().mimeTypePart())) {

				String body;
				body = getBodyAsString(response, headers);
				body = rule.mapToMockGitHub(body);

				builder.body(body);

			}
			builder.headers(new HttpHeaders(headers));

			return builder.build();
		}

		private String getBodyAsString(Response response, Collection<HttpHeader> headers) {
			String body;
			if (response.getHeaders().getHeader("Content-Encoding").containsValue("gzip")) {
				headers.removeIf(item -> item.keyEquals("Content-Encoding"));
				body = unGzipToString(response.getBody());
			} else {
				body = response.getBodyAsString();
			}
			return body;
		}

		private void fixListTraversalHeader(Response response, Collection<HttpHeader> headers) {
			// Lists are broken up into pages. The Link header contains urls for previous and next pages.
			HttpHeader linkHeader = response.getHeaders().getHeader("Link");
			if (linkHeader.isPresent()) {
				headers.removeIf(item -> item.keyEquals("Link"));
				headers.add(HttpHeader.httpHeader("Link", rule.mapToMockGitHub(linkHeader.firstValue())));
			}
		}

		private void fixLocationHeader(Response response, Collection<HttpHeader> headers) {
			// For redirects, the Location header points to the new target.
			HttpHeader linkHeader = response.getHeaders().getHeader("Location");
			if (linkHeader.isPresent()) {
				headers.removeIf(item -> item.keyEquals("Location"));
				headers.add(HttpHeader.httpHeader("Location", rule.mapToMockGitHub(linkHeader.firstValue())));
			}
		}

		@Override
		public String getName() {
			return "github-api-url-rewrite";
		}
	}

}
