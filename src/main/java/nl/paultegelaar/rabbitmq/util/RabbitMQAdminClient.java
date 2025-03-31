package nl.paultegelaar.rabbitmq.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import nl.paultegelaar.rabbitmq.config.Binding;
import nl.paultegelaar.rabbitmq.config.RabbitMQObjects;
import nl.paultegelaar.rabbitmq.config.VirtualHost;
import nl.paultegelaar.rabbitmq.util.config.ApplicationConfig;
import nl.paultegelaar.rabbitmq.util.exception.RabbitMQProvisioningException;

public class RabbitMQAdminClient {

	private static final String DURABLE_PROPERTY = "durable";
	private static final String ARGUMENTS_PROPERTY = "arguments";
	private static final String CONTENT_TYPE_HEADER = "Content-Type";
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final Logger LOGGER = Logger.getLogger(RabbitMQAdminClient.class.getName());

	private final ApplicationConfig applicationConfig;
	private final CloseableHttpClient httpClient;

	public RabbitMQAdminClient(ApplicationConfig applicationConfig) throws RabbitMQProvisioningException {
		Set<ConstraintViolation<ApplicationConfig>> violations = Validation.buildDefaultValidatorFactory()
				.getValidator().validate(applicationConfig);
		if (CollectionUtils.isNotEmpty(violations)) {
			throw new RabbitMQProvisioningException(violations.toString());
		}

		this.applicationConfig = applicationConfig;

		LOGGER.info("Building RabbitMQAdminClient");

		// Create config with basic timeout for request
		ConnectionConfig connConfig = ConnectionConfig.custom().build();
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(applicationConfig.getHttpRequestTimeout())
				.setConnectTimeout(applicationConfig.getHttpConnectionTimeout()).build();

		// Create connectionmanage, pool is overkill in this case, single one is fine
		BasicHttpClientConnectionManager httpClientConnectionManager = new BasicHttpClientConnectionManager();
		httpClientConnectionManager.setConnectionConfig(connConfig);

		// Build simple client
		httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
				.setConnectionManager(httpClientConnectionManager).build();

	}

	/**
	 * Validates the configuration, after all validations are passed, the RabbitMQ objects are created 
	 * 
	 * 
	 * @param rabbitMQObjectsToCreate
	 * @throws RabbitMQProvisioningException
	 */
	public void processRabbitMQConfig(RabbitMQObjects rabbitMQObjectsToCreate) throws RabbitMQProvisioningException {
		LOGGER.info(String.format("Processing new RabbitMQ configration: %s", rabbitMQObjectsToCreate.getConfigName()));
		Set<ConstraintViolation<RabbitMQObjects>> violations = Validation.buildDefaultValidatorFactory().getValidator()
				.validate(rabbitMQObjectsToCreate);
		if (CollectionUtils.isNotEmpty(violations)) {
			throw new RabbitMQProvisioningException(violations.toString());
		}

		if (CollectionUtils.isNotEmpty(rabbitMQObjectsToCreate.getVirtualHosts())) {
			LOGGER.info("Configuration doesn't contain any virtual hosts");
			return;
		}

		// Iterate through virtual hosts, If there are none, you are done
		List<VirtualHost> virtualHosts = rabbitMQObjectsToCreate.getVirtualHosts();
		LOGGER.info("Iterating through virtual hosts");
		for (VirtualHost virtualHost : virtualHosts) {
			// Check the name, it is needed to create all the underlying objects, also check
			// if there are any bindings
			if (StringUtils.isBlank(virtualHost.getName())|| CollectionUtils.isEmpty(virtualHost.getBindings())) {
					LOGGER.info("Configuration virtualhost name cannot be blank and virutalhost must contain bindings.");
					continue;
				}

				String virtualHostName = virtualHost.getName();

				LOGGER.info(String.format("Processing virtualhost with name: %s", virtualHostName));
				List<Binding> bindings = virtualHost.getBindings();
				for (Binding binding : bindings) {
					LOGGER.info("Processing bindings");
					if (StringUtils.isAnyBlank(binding.getExchangeName(), binding.getQueueName())) {
							LOGGER.info("Exchangename and queuename must never be blank.");
							continue;
						}
						
						String exchangeName = binding.getExchangeName();
						String queueName = binding.getQueueName();
						String routingkey = binding.getRoutingKey();

						LOGGER.info(String.format("Processing binding for queue with name: %s and exchange with name %s", queueName, exchangeName));

						performManagementAPICalls(virtualHostName, exchangeName, queueName, routingkey);

			}
		}

	}

	/**
	 * Perform calls to RabbitMQ Management API
	 * 
	 * @param virtualHostName
	 * @param exchangeName
	 * @param queueName
	 * @param routingkey
	 * @throws RabbitMQProvisioningException
	 */
	private void performManagementAPICalls(String virtualHostName, String exchangeName, String queueName, String routingkey) throws RabbitMQProvisioningException {
		try {
			LOGGER.info("Check if vhost exists");
			callRabbitMQManagementAPI(createVhostRequest(virtualHostName));

			LOGGER.info("Creating queue");
			callRabbitMQManagementAPI(
					createQueueRequest(virtualHostName, queueName, exchangeName, true));
			LOGGER.info("Creating dead letter queue");
			callRabbitMQManagementAPI(createDeadLetterQueueRequest(virtualHostName, queueName));

			if (StringUtils.startsWithIgnoreCase(exchangeName, applicationConfig.getReservedExchangeNamePrefix())) {
				LOGGER.info(String.format("Reserved exchange name, skipping create for: %s", exchangeName));
			} else {
				LOGGER.info("Creating exchange");
				callRabbitMQManagementAPI(createExchangeRequest(virtualHostName, exchangeName, "fanout"));
			}

			LOGGER.info("Creating binding");
			// Use the same name for routing key and queue
			callRabbitMQManagementAPI(createBindingRequest(virtualHostName, exchangeName, queueName, StringUtils.defaultIfBlank(routingkey, queueName)));
		} catch (URISyntaxException | IOException e) {
			throw new RabbitMQProvisioningException(e);
		}
	}

	/**
	 * Call RabbitMQ Management API, check if return code is in the 200 range and
	 * check if the response is at least a valid JSON
	 * 
	 * @param request
	 * @throws IOException
	 * @throws RabbitMQProvisioningException
	 */
	private void callRabbitMQManagementAPI(HttpUriRequest request) throws IOException, RabbitMQProvisioningException {
		LOGGER.info(String.format("Sending request to: %s", request.getURI().toString()));

		CloseableHttpResponse response = httpClient.execute(request);

		// Get response code
		int statusCode = response.getStatusLine().getStatusCode();

		String stringResponse = null;

		// Stupid simple check if response is JSON
		if (response.getEntity() != null) {
			stringResponse = EntityUtils.toString(response.getEntity());
			if (StringUtils.isNotBlank(stringResponse)) {
				LOGGER.info(String.format("Parsing response: %s", stringResponse));
				new JSONObject(stringResponse);
			}
		}
		// stupid simple validation of response code
		if (statusCode > 199 && statusCode < 300) {
			LOGGER.info(String.format("Succesfully created RabbitMQ object, received HTTP code: %s", statusCode));
		} else {
			throw new RabbitMQProvisioningException(
					String.format("Received invalid http response code: %s, body %s", statusCode, stringResponse));
		}
	}

	/**
	 * Create a generic PUT request based on payload, headers and URL
	 * 
	 * @param payload
	 * @param headers
	 * @param url
	 * @return HttpPut based on inputs
	 * @throws UnsupportedEncodingException
	 */
	private HttpPut createPutRequest(String payload, Map<String, String> headers, URI url)
			throws UnsupportedEncodingException {
		HttpPut request = new HttpPut(url);

		// Copy haeders to request
		if (MapUtils.isNotEmpty(headers)) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}

		// Set content as string, handle content type later
		request.setEntity(new StringEntity(payload));

		return request;
	}

	/**
	 * Create a generic POST request based on payload, headers and URL
	 * 
	 * 
	 * @param payload
	 * @param headers
	 * @param url
	 * @return HttpPost based on inputs
	 * @throws UnsupportedEncodingException
	 */
	private HttpPost createPostRequest(String payload, Map<String, String> headers, URI url)
			throws UnsupportedEncodingException {
		HttpPost request = new HttpPost(url);

		// Copy haeders to request
		if (MapUtils.isNotEmpty(headers)) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}

		// Set content as string, handle content type later
		request.setEntity(new StringEntity(payload));

		return request;
	}

	/**
	 * Create a generic GET request based on headers and URL
	 * 
	 * 
	 * @param payload
	 * @param headers
	 * @param url
	 * @return HttpGet based on inputs
	 * @throws UnsupportedEncodingException
	 */
	private HttpGet createGetRequest(Map<String, String> headers, URI url) {
		HttpGet request = new HttpGet(url);

		// Copy haeders to request
		if (MapUtils.isNotEmpty(headers)) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}

		return request;
	}

	/**
	 * Create basic auth credentials based on configs
	 * 
	 * @return String containing base64 encoded credentials including Basic prefix.
	 */
	private String buildBasicAuthCredentials() {

		// Encode username and password
		String credentials = Base64.getEncoder().encodeToString(
				(applicationConfig.getApiUsername() + ":" + new String(applicationConfig.getApiPassword()))
						.getBytes(StandardCharsets.UTF_8));
		return "Basic ".concat(credentials);

	}

	private HttpUriRequest createExchangeRequest(String virtualhostName, String exchangeName, String exchangeType)
			throws UnsupportedEncodingException, URISyntaxException {

		// Build json message
		JSONObject json = new JSONObject();
		json.put("type", exchangeType);

		// Add headers
		Map<String, String> headers = new HashMap<>();
		headers.put(AUTHORIZATION_HEADER, buildBasicAuthCredentials());
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		// Build URL
		URI fullUrl = createURL(applicationConfig.getApiBaseURL(), applicationConfig.getExchangePath(),
				URLEncoder.encode(virtualhostName, StandardCharsets.UTF_8), exchangeName);

		// Return with combined URL
		return createPutRequest(json.toString(), headers, fullUrl);
	}

	/**
	 * Create a PUT request to create or update queue.
	 * 
	 * @param virtualhost
	 * @param queueName
	 * @param exchangeName
	 * @return HttpPut containing the body, URL and headers needed to create a queue
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 */
	private HttpUriRequest createQueueRequest(String virtualhost, String queueName, String exchangeName,
			boolean durable) throws UnsupportedEncodingException, URISyntaxException {

		// Build main json message
		JSONObject json = new JSONObject();
		json.put(DURABLE_PROPERTY, durable);
		// Build arguments json object
		JSONObject arguments = new JSONObject();
		arguments.put("x-dead-letter-exchange", exchangeName);
		arguments.put("x-dead-letter-routing-key", queueName.concat(applicationConfig.getDeadLetterPostfix()));
		// Add arguments to main json
		json.put(ARGUMENTS_PROPERTY, arguments);

		// Add headers
		Map<String, String> headers = new HashMap<>();
		headers.put(AUTHORIZATION_HEADER, buildBasicAuthCredentials());
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		// Build URL
		URI fullUrl = createURL(applicationConfig.getApiBaseURL(), applicationConfig.getQueuePath(),
				URLEncoder.encode(virtualhost, StandardCharsets.UTF_8),
				URLEncoder.encode(queueName, StandardCharsets.UTF_8));

		// Return with combined URL, headers and json body
		return createPutRequest(json.toString(), headers, fullUrl);
	}

	/**
	 * Create a PUT request to create or update dead letter queue.
	 * 
	 * @param virtualhost
	 * @param queueName
	 * @return HttpPut containing the body, URL and headers needed to create a dead
	 *         letter queue
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 */
	private HttpUriRequest createDeadLetterQueueRequest(String virtualhost, String queueName)
			throws UnsupportedEncodingException, URISyntaxException {

		// Build json message
		JSONObject json = new JSONObject();
		json.put(DURABLE_PROPERTY, true);
		// Arguments is default empty
		json.put(ARGUMENTS_PROPERTY, new JSONObject());

		// Add headers
		Map<String, String> headers = new HashMap<>();
		headers.put(AUTHORIZATION_HEADER, buildBasicAuthCredentials());
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		// Build URL
		URI fullUrl = createURL(applicationConfig.getApiBaseURL(), applicationConfig.getDeadLetterQueuePath(),
				URLEncoder.encode(virtualhost, StandardCharsets.UTF_8),
				URLEncoder.encode(queueName.concat(applicationConfig.getDeadLetterPostfix()), StandardCharsets.UTF_8));

		// Return with combined URL, headers and json body
		return createPutRequest(json.toString(), headers, fullUrl);
	}

	/**
	 * Create a POST request to create a binding.
	 * 
	 * @param virtualhostName
	 * @param exchangeName
	 * @param queueName
	 * @param routingKey
	 * @return HttpPost containing the body, URL and headers needed to create a
	 *         binding
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 */
	private HttpUriRequest createBindingRequest(String virtualhostName, String exchangeName, String queueName,
			String routingKey) throws UnsupportedEncodingException, URISyntaxException {

		// Build json message
		JSONObject json = new JSONObject();
		json.put("routing_key", routingKey);
		// Arguments is default empty
		json.put(ARGUMENTS_PROPERTY, new JSONObject());

		// Add headers
		Map<String, String> headers = new HashMap<>();
		headers.put(AUTHORIZATION_HEADER, buildBasicAuthCredentials());
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		// Build URL
		URI fullUrl = createURL(applicationConfig.getApiBaseURL(), applicationConfig.getBindingPath(),
				URLEncoder.encode(virtualhostName, StandardCharsets.UTF_8),
				URLEncoder.encode(exchangeName, StandardCharsets.UTF_8),
				URLEncoder.encode(queueName, StandardCharsets.UTF_8));

		return createPostRequest(json.toString(), headers, fullUrl);
	}

	/**
	 * Create GET request used to check if a vhost exists
	 * 
	 * @param virtualhostName
	 * @return HttpGet based on virtualhost name
	 * @throws URISyntaxException
	 */
	private HttpUriRequest createVhostRequest(String virtualhostName) throws URISyntaxException {

		// Add headers
		Map<String, String> headers = new HashMap<>();
		headers.put(AUTHORIZATION_HEADER, buildBasicAuthCredentials());

		// Build URL
		URI fullUrl = createURL(applicationConfig.getApiBaseURL(), applicationConfig.getVhostPath(),
				URLEncoder.encode(virtualhostName, StandardCharsets.UTF_8));

		return createGetRequest(headers, fullUrl);
	}

	/**
	 * Create URL based on a baseURL a path and any parameters
	 * 
	 * @param baseURL
	 * @param path
	 * @param params
	 * @return URI with params inserted
	 * @throws URISyntaxException
	 */
	private URI createURL(String baseURL, String path, Object... params) throws URISyntaxException {
		return new URI(baseURL.concat(String.format(path, params)));
	}

}
