package com.cicd.demo.employee_service;

import com.cicd.demo.employee_service.model.Employee;
import com.cicd.demo.employee_service.repository.EmployeeRepository;
import com.cicd.demo.employee_service.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmployeeService.
 *
 * Key ideas:
 * @ExtendWith(MockitoExtension.class) → enables Mockito without Spring context (fast!)
 * @Mock    → creates a fake EmployeeRepository (no DB needed)
 * @InjectMocks → creates the real EmployeeService and injects the mock into it
 *
 * These tests run in ~200ms. Jenkins will run them on every commit.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private EmployeeRepository employeeRepository;

	@InjectMocks
	private EmployeeService employeeService;

	private Employee sampleEmployee;

	@BeforeEach
	void setUp() {
		// Builds a sample employee used across multiple tests
		sampleEmployee = Employee.builder()
				.id(1L)
				.name("Alice Smith")
				.email("alice@example.com")
				.department("Engineering")
				.salary(75000.0)
				.build();
	}

	// ── GET ALL ──────────────────────────────────────────────────────────────

	@Test
	@DisplayName("getAllEmployees should return all employees from repository")
	void getAllEmployees_returnsAllEmployees() {
		when(employeeRepository.findAll()).thenReturn(List.of(sampleEmployee));

		List<Employee> result = employeeService.getAllEmployees();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Alice Smith");
		verify(employeeRepository, times(1)).findAll();
	}

	// ── GET BY ID ────────────────────────────────────────────────────────────

	@Test
	@DisplayName("getEmployeeById should return employee when found")
	void getEmployeeById_whenExists_returnsEmployee() {
		when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

		Employee result = employeeService.getEmployeeById(1L);

		assertThat(result.getEmail()).isEqualTo("alice@example.com");
	}

	@Test
	@DisplayName("getEmployeeById should throw exception when not found")
	void getEmployeeById_whenNotFound_throwsException() {
		when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Employee not found with id: 99");
	}

	// ── CREATE ───────────────────────────────────────────────────────────────

	@Test
	@DisplayName("createEmployee should save and return new employee")
	void createEmployee_withNewEmail_savesSuccessfully() {
		when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(false);
		when(employeeRepository.save(sampleEmployee)).thenReturn(sampleEmployee);

		Employee result = employeeService.createEmployee(sampleEmployee);

		assertThat(result.getId()).isEqualTo(1L);
		verify(employeeRepository).save(sampleEmployee);
	}

	@Test
	@DisplayName("createEmployee should throw when email already exists")
	void createEmployee_withDuplicateEmail_throwsException() {
		when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(true);

		assertThatThrownBy(() -> employeeService.createEmployee(sampleEmployee))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Employee already exists with email");

		// Ensure save() was never called — no partial writes
		verify(employeeRepository, never()).save(any());
	}

	// ── DELETE ───────────────────────────────────────────────────────────────

	@Test
	@DisplayName("deleteEmployee should call deleteById when employee exists")
	void deleteEmployee_whenExists_deletesSuccessfully() {
		when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
		doNothing().when(employeeRepository).deleteById(1L);

		assertThatCode(() -> employeeService.deleteEmployee(1L))
				.doesNotThrowAnyException();

		verify(employeeRepository).deleteById(1L);
	}

	// ── UPDATE ───────────────────────────────────────────────────────────────

	@Test
	@DisplayName("updateEmployee should apply all field changes and return saved entity")
	void updateEmployee_whenExists_updatesAllFields() {
		Employee updated = Employee.builder()
				.name("Alice Johnson")
				.email("alice.j@example.com")
				.department("Product")
				.salary(90000.0)
				.build();

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
		when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

		Employee result = employeeService.updateEmployee(1L, updated);

		assertThat(result.getName()).isEqualTo("Alice Johnson");
		assertThat(result.getDepartment()).isEqualTo("Product");
		assertThat(result.getSalary()).isEqualTo(90000.0);
	}
}

