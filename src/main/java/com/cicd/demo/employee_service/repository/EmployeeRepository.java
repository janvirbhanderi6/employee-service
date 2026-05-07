package com.cicd.demo.employee_service.repository;

import com.cicd.demo.employee_service.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * EmployeeRepository — our database access layer.
 *
 * By extending JpaRepository we get these methods FOR FREE (no SQL needed):
 *   save(), findById(), findAll(), deleteById(), existsById(), count() ...
 *
 * We only need to declare methods that aren't already provided.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Spring Data generates the SQL from the method name:
    // SELECT * FROM employees WHERE email = ?
    Optional<Employee> findByEmail(String email);

    // SELECT * FROM employees WHERE department = ?
    List<Employee> findByDepartment(String department);

    // SELECT COUNT(*) > 0 FROM employees WHERE email = ?
    boolean existsByEmail(String email);
}