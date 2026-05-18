package vn.amela.employeeservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;
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
        CreateEmployeeRequest request = validRequest();
        Department department = activeDepartment();
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
        Department inactiveDepartment = activeDepartment();
        inactiveDepartment.setIsActive(false);
        when(departmentMapper.findById(1L)).thenReturn(inactiveDepartment);

        assertThatThrownBy(() -> employeeService.create(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Department not found");

        verify(employeeMapper, never()).insert(any(Employee.class));
        verify(outboxEventMapper, never()).insert(any(OutboxEvent.class));
    }

    @Test
    void createRejectsAuthUserAlreadyAssignedToAnotherEmployee() {
        when(departmentMapper.findById(1L)).thenReturn(activeDepartment());
        when(employeeMapper.findByAuthUserId(99L)).thenReturn(createdEmployee());

        assertThatThrownBy(() -> employeeService.create(validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Auth user already assigned to an employee");

        verify(employeeMapper, never()).insert(any(Employee.class));
        verify(outboxEventMapper, never()).insert(any(OutboxEvent.class));
    }

    private CreateEmployeeRequest validRequest() {
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

    private Department activeDepartment() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        department.setIsActive(true);
        return department;
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
