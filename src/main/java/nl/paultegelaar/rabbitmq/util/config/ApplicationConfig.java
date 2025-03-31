package nl.paultegelaar.rabbitmq.util.config;


import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ApplicationConfig {

	private int httpConnectionTimeout = 60000;
	private int httpRequestTimeout = 60000;
	private String apiBaseURL;
	private String apiUsername;
	private char[] apiPassword;
	private String exchangePath = "/api/exchanges/%s/%s";
	private String queuePath = "/api/queues/%s/%s";
	private String deadLetterQueuePath = "/api/queues/%s/%s";
	private String bindingPath = "/api/bindings/%s/e/%s/q/%s";
	private String vhostPath = "/api/vhosts/%s/";
	
	private String deadLetterPostfix = ".dead-letter";
	private String reservedExchangeNamePrefix = "amq.";
	
	
	@NotBlank
	public String getVhostPath() {
		return vhostPath;
	}

	public void setVhostPath(String vhostPath) {
		this.vhostPath = vhostPath;
	}

	@NotBlank		
	public String getDeadLetterPostfix() {
		return deadLetterPostfix;
	}

	public void setDeadLetterPostfix(String deadLetterPostfix) {
		this.deadLetterPostfix = deadLetterPostfix;
	}

	@NotBlank	
	public String getBindingPath() {
		return bindingPath;
	}

	public void setBindingPath(String bindingPath) {
		this.bindingPath = bindingPath;
	}

	@NotBlank	
	public String getDeadLetterQueuePath() {
		return deadLetterQueuePath;
	}

	public void setDeadLetterQueuePath(String deadLetterQueuePath) {
		this.deadLetterQueuePath = deadLetterQueuePath;
	}

	@NotBlank
	public String getQueuePath() {
		return queuePath;
	}

	public void setQueuePath(String queuePath) {
		this.queuePath = queuePath;
	}

	@NotBlank
	public String getExchangePath() {
		return exchangePath;
	}

	public void setExchangePath(String exchangePath) {
		this.exchangePath = exchangePath;
	}

	@NotNull
	@URL
	public String getApiBaseURL() {
		return apiBaseURL;
	}

	public void setApiBaseURL(String apiBaseURL) {
		this.apiBaseURL = apiBaseURL;
	}

	@NotBlank
	public String getApiUsername() {
		return apiUsername;
	}

	public void setApiUsername(String apiUsername) {
		this.apiUsername = apiUsername;
	}

	
	@NotNull
	public char[] getApiPassword() {
		return apiPassword;
	}

	public void setApiPassword(char[] apiPassword) {
		this.apiPassword = apiPassword;
	}

	@NotNull
	@Min(value = 1L)
	public int getHttpConnectionTimeout() {
		return httpConnectionTimeout;
	}

	public void setHttpConnectionTimeout(int httpConnectionTimeout) {
		this.httpConnectionTimeout = httpConnectionTimeout;
	}

	
	@NotNull
	@Min(value = 1L)
	public int getHttpRequestTimeout() {
		return httpRequestTimeout;
	}

	public void setHttpRequestTimeout(int httpRequestTimeout) {
		this.httpRequestTimeout = httpRequestTimeout;
	}

	@NotNull
	public String getReservedExchangeNamePrefix() {
		return reservedExchangeNamePrefix;
	}

	public void setReservedExchangeNamePrefix(String reservedExchangeNamePrefix) {
		this.reservedExchangeNamePrefix = reservedExchangeNamePrefix;
	}
	
	
	
	
	
}
