package nl.paultegelaar.rabbitmq;


import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.paultegelaar.rabbitmq.config.RabbitMQObjects;
import nl.paultegelaar.rabbitmq.util.RabbitMQAdminClient;
import nl.paultegelaar.rabbitmq.util.config.ApplicationConfig;
import nl.paultegelaar.rabbitmq.util.exception.RabbitMQProvisioningException;

public class RabbitMQObjectCreator {
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	

	public static void main(String[] args) {
		
		//Create options
		Options options = buildOptions();
		
		//Build commandline
		CommandLine cmd;
	    CommandLineParser parser = new DefaultParser();
	    HelpFormatter helper = new HelpFormatter();
		
	    //Run it
	    try {
	    	//Parse options
	        cmd = parser.parse(options, args);
	        String adminEndpoint = cmd.getOptionValue("endpoint");
	        String username = cmd.getOptionValue("username");
	        String password = cmd.getOptionValue("password");
	        String configFile = cmd.getOptionValue("configFile");
	        	  
	        //Read config file
	        RabbitMQObjects rabbitMQObjects = OBJECT_MAPPER.readValue(new File(configFile), RabbitMQObjects.class);
	        
	        //Build application config
	        ApplicationConfig applicationConfig = new ApplicationConfig();
			applicationConfig.setApiBaseURL(adminEndpoint);
			applicationConfig.setApiUsername(username);
			applicationConfig.setApiPassword(password.toCharArray());
	        
			//Create client
			RabbitMQAdminClient rabbitMQAdminClient = new RabbitMQAdminClient(applicationConfig);
			
			//Run provisioning
			rabbitMQAdminClient.processRabbitMQConfig(rabbitMQObjects);
	        
	    } catch (ParseException e) {
	        System.out.println(e.getMessage());
	        helper.printHelp("Usage:", options);
	        System.exit(0);
	    }  catch (IOException e) {
	    	System.out.println("Unable to read config file: " + e.getMessage());
	        System.exit(0);
		} catch (RabbitMQProvisioningException e) {
			System.out.println("Error while running RabbitMQ provisioning: " + e.getMessage());
	        System.exit(0);
		}
	    
		
	}

	private static Options buildOptions() {
		Options options = new Options();
		
		Option adminEndpoint = Option.builder("e").longOpt("endpoint")
		        						   .argName("endpoint")
		        						   .hasArg()
		        						   .required(true)
		        						   .desc("Set RabbitMQ admin endpoint").build();
		
		Option username = Option.builder("u").longOpt("username")
										   .argName("username")
										   .hasArg()
										   .required(true)
										   .desc("Set RabbitMQ admin username").build();
		
		Option password = Option.builder("p").longOpt("password")
										   .argName("password")
										   .hasArg()
										   .required(true)
										   .desc("Set RabbitMQ admin password").build();
		
		Option configFile = Option.builder("c").longOpt("configFile")
				   .argName("configFile")
				   .hasArg()
				   .required(true)
				   .desc("Set config file containing objects").build();
		

		return options.addOption(password).addOption(username).addOption(adminEndpoint).addOption(configFile);
	}

}