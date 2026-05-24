package vn.amela.employeeservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.DuplicateResourceException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.mapper.OutboxEventMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private OutboxEventMapper outboxEventMapper;

    private EmployeeServiceImpl employeeService;
    private JsonMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        employeeService = new EmployeeServiceImpl(
                employeeMapper,
                departmentMapper,
                outboxEventMapper,
                objectMapper
        );
    }

    @Test
    void createNormalizesInputPersistsEmployeeAndStoresOutboxEvent() {
        CreateEmployeeRequest request = validCreateRequest();
        Department department = activeDepartment(1L, "Engineering");
        Employee createdEmployee = createdEmployee();

        when(departmentMapper.findById(1L)).thenReturn(department);
        doAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(10L);
            return null;
        }).when(employeeMapper).insert(any(Employee.class));
        when(employeeMapper.findById(10L)).thenReturn(createdEmployee);

        EmployeeResponse response = employeeService.create(request);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).insert(employeeCaptor.capture());
        Employee insertedEmployee = employeeCaptor.getValue();
        assertThat(insertedEmployee.getEmployeeCode()).isEqualTo("EMP010");
        assertThat(insertedEmployee.getEmail()).isEqualTo("new.employee@company.com");
        assertThat(insertedEmployee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(outboxCaptor.capture());
        OutboxEvent outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.getAggregateType()).isEqualTo("Employee");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(10L);
        assertThat(outboxEvent.getEventType()).isEqualTo("employee.created");

        JsonNode payload = objectMapper.readTree(outboxEvent.getPayload());
        assertThat(payload.get("eventType").asString()).isEqualTo("employee.created");
        assertThat(payload.get("aggregateId").asLong()).isEqualTo(10L);
        assertThat(payload.get("employeeCode").asString()).isEqualTo("EMP010");
        assertThat(payload.get("email").asString()).isEqualTo("new.employee@company.com");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.departmentName()).isEqualTo("Engineering");
        assertThat(response.createdAt()).isEqualTo(createdEmployee.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(createdEmployee.getUpdatedAt());
    }

    @Test
    void createRejectsInactiveDepartment() {
        Department inactiveDepartment = activeDepartment(1L, "Engineering");
        inactiveDepartment.setIsActive(false);
        when(departmentMapper.findById(1L)).thenReturn(inactiveDepartment);

        assertThatThrownBy(() -> employeeService.create(validCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Department not found");

        verify(employeeMapper, never()).insert(any(Employee.class));
        verify(outboxEventMapper, never()).insert(any(OutboxEvent.class));
    }

    @Test
    void createRejectsAuthUserAlreadyAssignedToAnotherEmployee() {
        when(departmentMapper.findById(1L)).thenReturn(activeDepartment(1L, "Engineering"));
        when(employeeMapper.findByAuthUserId(99L)).thenReturn(createdEmployee());

        assertThatThrownBy(() -> employeeService.create(validCreateRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Auth user already assigned to an employee");

        verify(employeeMapper, never()).insert(any(Employee.class));
        verify(outboxEventMapper, never()).insert(any(OutboxEvent.class));
    }

    @Test
    void updateByHrUpdatesEmployeeWithoutOutboxEventWhenDepartmentAndSalaryAreUnchanged() {
        Employee existingEmployee = existingEmployee();
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee);
        when(departmentMapper.findById(2L)).thenReturn(activeDepartment(2L, "IT"));

        UpdateEmployeeRequest request = updateEmployeeRequest(2L, new BigDecimal("1000.00"));

        EmployeeResponse response = employeeService.updateByHr(1L, request);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).updateByHr(employeeCaptor.capture());
        Employee updatedEmployee = employeeCaptor.getValue();
        assertThat(updatedEmployee.getFullName()).isEqualTo("John Doe");
        assertThat(updatedEmployee.getEmail()).isEqualTo("john@example.com");
        assertThat(updatedEmployee.getPhone()).isEqualTo("123456789");
        assertThat(updatedEmployee.getDepartmentId()).isEqualTo(2L);
        assertThat(updatedEmployee.getAuthUserId()).isEqualTo(99L);
        assertThat(response.departmentName()).isEqualTo("IT");
        verify(outboxEventMapper, never()).insert(any(OutboxEvent.class));
    }

    @Test
    void updateByHrStoresOutboxEventWhenDepartmentOrSalaryChanges() {
        Employee existingEmployee = existingEmployee();
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee);
        when(departmentMapper.findById(3L)).thenReturn(activeDepartment(3L, "HR"));

        employeeService.updateByHr(1L, updateEmployeeRequest(3L, new BigDecimal("1500.00")));

        verify(employeeMapper).updateByHr(any(Employee.class));
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("EMPLOYEE");
        assertThat(event.getAggregateId()).isEqualTo(1L);
        assertThat(event.getEventType()).isEqualTo("employee.status.changed");

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("id").asLong()).isEqualTo(1L);
        assertThat(payload.get("departmentId").asLong()).isEqualTo(3L);
        assertThat(payload.get("email").asString()).isEqualTo("john@example.com");
    }

    @Test
    void updateByHrRejectsDuplicateEmailFromDatabaseConstraint() {
        Employee existingEmployee = existingEmployee();
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee);
        when(departmentMapper.findById(2L)).thenReturn(activeDepartment(2L, "IT"));
        when(employeeMapper.updateByHr(any(Employee.class)))
                .thenThrow(new DuplicateKeyException("duplicate email"));

        assertThatThrownBy(() -> employeeService.updateByHr(
                1L,
                updateEmployeeRequest(2L, new BigDecimal("1000.00"))
        ))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists: john@example.com");
    }

    @Test
    void updateByHrRejectsInactiveDepartment() {
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee());
        Department inactiveDepartment = activeDepartment(2L, "IT");
        inactiveDepartment.setIsActive(false);
        when(departmentMapper.findById(2L)).thenReturn(inactiveDepartment);

        assertThatThrownBy(() -> employeeService.updateByHr(
                1L,
                updateEmployeeRequest(2L, new BigDecimal("1000.00"))
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Department not found");

        verify(employeeMapper, never()).updateByHr(any(Employee.class));
        verifyNoInteractions(outboxEventMapper);
    }

    @Test
    void updateContactUpdatesOwnerContactAndReturnsDepartmentName() {
        Employee existingEmployee = existingEmployee();
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee);
        when(departmentMapper.findById(2L)).thenReturn(activeDepartment(2L, "IT"));

        EmployeeResponse response = employeeService.updateContact(
                1L,
                new UpdateContactRequest(" new@example.com ", " 987654321 "),
                99L
        );

        verify(employeeMapper).updateContact(1L, "new@example.com", "987654321");
        assertThat(existingEmployee.getEmail()).isEqualTo("new@example.com");
        assertThat(existingEmployee.getPhone()).isEqualTo("987654321");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.phone()).isEqualTo("987654321");
        assertThat(response.departmentName()).isEqualTo("IT");
    }

    @Test
    void updateContactRejectsRequesterThatDoesNotOwnEmployeeAuthUser() {
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee());

        assertThatThrownBy(() -> employeeService.updateContact(
                1L,
                new UpdateContactRequest("new@example.com", "987654321"),
                100L
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You are not authorized to update this employee's contact");

        verify(employeeMapper, never()).updateContact(any(), any(), any());
        verifyNoInteractions(departmentMapper, outboxEventMapper);
    }

    @Test
    void updateContactRejectsDuplicateEmailFromDatabaseConstraint() {
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee());
        when(employeeMapper.updateContact(1L, "new@example.com", "987654321"))
                .thenThrow(new DuplicateKeyException("duplicate email"));

        assertThatThrownBy(() -> employeeService.updateContact(
                1L,
                new UpdateContactRequest("new@example.com", "987654321"),
                99L
        ))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists: new@example.com");

        verifyNoInteractions(departmentMapper, outboxEventMapper);
    }

    private CreateEmployeeRequest validCreateRequest() {
        return new CreateEmployeeRequest(
                " emp010 ",
                " New Employee ",
                " NEW.EMPLOYEE@company.com ",
                " 0900000000 ",
                " Backend Developer ",
                1L,
                99L,
                new BigDecimal("12000000.00"),
                LocalDate.of(2026, 5, 18)
        );
    }

    private UpdateEmployeeRequest updateEmployeeRequest(Long departmentId, BigDecimal salary) {
        return new UpdateEmployeeRequest(
                " John Doe ",
                " JOHN@EXAMPLE.COM ",
                " 123456789 ",
                " Dev ",
                departmentId,
                salary,
                LocalDate.of(2026, 5, 18)
        );
    }

    private Department activeDepartment(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setIsActive(true);
        return department;
    }

    private Employee existingEmployee() {
        return Employee.builder()
                .id(1L)
                .employeeCode("EMP001")
                .fullName("Existing Employee")
                .email("existing@example.com")
                .phone("0900000000")
                .position("Backend Developer")
                .departmentId(2L)
                .authUserId(99L)
                .salary(new BigDecimal("1000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .status(EmployeeStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
    }

    private Employee createdEmployee() {
        return Employee.builder()
                .id(10L)
                .employeeCode("EMP010")
                .fullName("New Employee")
                .email("new.employee@company.com")
                .phone("0900000000")
                .position("Backend Developer")
                .departmentId(1L)
                .authUserId(99L)
                .salary(new BigDecimal("12000000.00"))
                .startDate(LocalDate.of(2026, 5, 18))
                .status(EmployeeStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 5, 18, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 18, 9, 0))
                .build();
    }
}
