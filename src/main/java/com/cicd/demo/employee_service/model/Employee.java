package com.cicd.demo.employee_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Employee — JPA entity mapped to the "employees" table in PostgreSQL.
 *
 * @Entity  → tells JPA this class represents a database table
 * @Table   → explicitly names the table (good practice)
 * Lombok annotations remove all the boilerplate getter/setter/constructor code
 */

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder                   // lets us do: Employee.builder().name("Alice").build()
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto-increment PK
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Department is required")
    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Double salary;
}
