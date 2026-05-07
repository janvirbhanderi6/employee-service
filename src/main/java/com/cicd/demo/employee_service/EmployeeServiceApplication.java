package com.cicd.demo.employee_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EmployeeServiceApplication — the entry point of the entire Spring Boot app.
 *
 * @SpringBootApplication is a shortcut for three annotations:
 *   @Configuration      → this class can define Spring beans
 *   @EnableAutoConfiguration → Spring Boot auto-configures based on dependencies
 *   @ComponentScan      → scans this package and subpackages for @Service, @Repository, etc.
 */

@SpringBootApplication
public class EmployeeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmployeeServiceApplication.class, args);
	}

}
