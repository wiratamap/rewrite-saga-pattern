package com.mybank.servicetransaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.mybank.servicetransaction.*")
public class ServiceTransactionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceTransactionApplication.class, args);
	}

}
