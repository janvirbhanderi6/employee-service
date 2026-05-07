package com.cicd.demo.employee_service.service;

import com.cicd.demo.employee_service.model.Employee;
import com.cicd.demo.employee_service.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * EmployeeService — all business rules live here, not in the controller.
 *
 * Why separate service from controller?
 *   → Controller only handles HTTP (request/response)
 *   → Service handles logic (validation, DB calls, exceptions)
 *   → This separation makes unit testing much easier (we can test logic without HTTP)
 *
 * @Slf4j   → injects a `log` field via Lombok (used by ELK stack in Step 9)
 * @Service → marks this as a Spring-managed bean
 */

@Service
@RequiredArgsConstructor   // Lombok: generates a constructor for all final fields (injected by Spring)
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

// ── READ ──────────────────────────────────────────────────────────────

    public List<Employee> getAllEmployees() {
        log.info("Fetching all employees");
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Long id) {
        log.info("Fetching employee with id: {}", id);
        return employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Employee not found with id: {}", id);
                    return new RuntimeException("Employee not found with id: " + id);
                });
    }

    public List<Employee> getEmployeesByDepartment(String department) {
        log.info("Fetching employees in department: {}", department);
        return employeeRepository.findByDepartment(department);
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    @Transactional   // if anything fails, the whole DB operation is rolled back
    public Employee createEmployee(Employee employee) {
        // Business rule: no two employees can share an email
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            log.warn("Attempt to create duplicate email: {}", employee.getEmail());
            throw new RuntimeException("Employee already exists with email: " + employee.getEmail());
        }
        Employee saved = employeeRepository.save(employee);
        log.info("Created employee with id: {}", saved.getId());
        return saved;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    @Transactional
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existing = getEmployeeById(id);   // throws if not found

        existing.setName(updatedEmployee.getName());
        existing.setEmail(updatedEmployee.getEmail());
        existing.setDepartment(updatedEmployee.getDepartment());
        existing.setSalary(updatedEmployee.getSalary());

        Employee saved = employeeRepository.save(existing);
        log.info("Updated employee with id: {}", id);
        return saved;
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    @Transactional
    public void deleteEmployee(Long id) {
        // Ensure it exists before deleting (gives a clear error if not)
        getEmployeeById(id);
        employeeRepository.deleteById(id);
        log.info("Deleted employee with id: {}", id);
    }
}

