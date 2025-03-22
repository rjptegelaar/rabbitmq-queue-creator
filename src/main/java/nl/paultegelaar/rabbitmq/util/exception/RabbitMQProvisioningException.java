package nl.paultegelaar.rabbitmq.util.exception;

public class RabbitMQProvisioningException extends Exception{

	private static final long serialVersionUID = 2535785538516049548L;

	public RabbitMQProvisioningException(String message) {
		super(message);
	}
	
	public RabbitMQProvisioningException(Exception exception) {
		super(exception);
	}
	
}
