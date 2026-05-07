package com.cicd.demo.employee_service.controller;

import com.cicd.demo.employee_service.model.Employee;
import com.cicd.demo.employee_service.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EmployeeController — exposes REST endpoints under /api/employees
 *
 * @RestController → combines @Controller + @ResponseBody (returns JSON automatically)
 * @RequestMapping → base path for all endpoints in this class
 * @Valid          → triggers the validation rules defined in the Employee model
 */

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // GET /api/employees
    // Returns all employees — HTTP 200
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // GET /api/employees/{id}
    // Returns one employee by ID — HTTP 200, or 404 if not found
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // GET /api/employees/department/{dept}
    // Returns all employees in a department
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Employee>> getByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(employeeService.getEmployeesByDepartment(department));
    }

    // POST /api/employees
    // Creates a new employee — HTTP 201 Created
    @PostMapping
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) {
        Employee created = employeeService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/employees/{id}
    // Fully updates an employee — HTTP 200
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody Employee employee) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, employee));
    }

    // DELETE /api/employees/{id}
    // Deletes an employee — HTTP 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
