package vn.amela.employeeservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.amela.employeeservice.client.LeaveServiceClient;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
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
import java.util.List;

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

    @Mock
    private LeaveServiceClient leaveServiceClient;

    private EmployeeServiceImpl employeeService;
    private JsonMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        employeeService = new EmployeeServiceImpl(
                employeeMapper,
                departmentMapper,
                outboxEventMapper,
                objectMapper,
                leaveServiceClient
        );
    }

    @Test
    void createNormalizesInputPersistsEmployeeAndStoresOutboxEvent() {
        when(departmentMapper.findById(1L)).thenReturn(activeDepartment());
        doAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(10L);
            return null;
        }).when(employeeMapper).insert(any(Employee.class));
        when(employeeMapper.findById(10L)).thenReturn(createdEmployee());

        EmployeeResponse response = employeeService.create(validRequest());

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).insert(employeeCaptor.capture());
        assertThat(employeeCaptor.getValue().getEmployeeCode()).isEqualTo("EMP010");
        assertThat(employeeCaptor.getValue().getEmail()).isEqualTo("new.employee@company.com");
        assertThat(employeeCaptor.getValue().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);

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
    }

    @Test
    void searchDelegatesToMapperAndBuildsPageResponse() {
        EmployeeFilterRequest filter = EmployeeFilterRequest.builder()
                .likeName("  An  ")
                .departmentId(1L)
                .position(" Backend Developer ")
                .status(EmployeeStatus.ACTIVE)
                .startDateFrom(LocalDate.of(2024, 1, 1))
                .startDateTo(LocalDate.of(2024, 12, 31))
                .page(1)
                .size(2)
                .sortBy("employeeCode")
                .sortDirection("ASC")
                .build();
        Employee employee = Employee.builder()
                .id(10L)
                .employeeCode("EMP010")
                .fullName("Nguyen Van An")
                .email("an@company.com")
                .position("Backend Developer")
                .departmentId(1L)
                .authUserId(20L)
                .status(EmployeeStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 15))
                .build();
        when(employeeMapper.search("An", 1L, "Backend Developer", EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), "employeeCode", "asc", 2, 2))
                .thenReturn(List.of(employee));
        when(employeeMapper.countByFilter("An", 1L, "Backend Developer", EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).thenReturn(5);
        when(departmentMapper.findById(1L)).thenReturn(activeDepartment());

        PageResponse<EmployeeResponse> response = employeeService.search(filter);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.items()).extracting(EmployeeResponse::departmentName).containsExactly("Engineering");
    }

    @Test
    void searchNullFilterUsesDefaults() {
        when(employeeMapper.search(null, null, null, null, null, null, "createdAt", "desc", 0, 10))
                .thenReturn(List.of());
        when(employeeMapper.countByFilter(null, null, null, null, null, null)).thenReturn(0);

        PageResponse<EmployeeResponse> response = employeeService.search(null);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.items()).isEmpty();
        verifyNoInteractions(departmentMapper);
    }

    @Test
    void searchInvalidDateRangeThrows() {
        EmployeeFilterRequest filter = EmployeeFilterRequest.builder()
                .startDateFrom(LocalDate.of(2024, 12, 31))
                .startDateTo(LocalDate.of(2024, 1, 1))
                .build();

        assertThatThrownBy(() -> employeeService.search(filter)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void updateByHrWithoutDepartmentOrSalaryChangeDoesNotStoreOutboxEvent() {
        Employee existingEmployee = employeeForUpdate();
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee);
        when(departmentMapper.findById(2L)).thenReturn(department(2L, "IT", true));

        EmployeeResponse response = employeeService.updateByHr(1L, updateRequest(2L, BigDecimal.valueOf(1000)));

        verify(employeeMapper).updateByHr(any(Employee.class));
        verify(outboxEventMapper, never()).insert(any(OutboxEvent.class));
        assertThat(response).isNotNull();
    }

    @Test
    void updateByHrWithDepartmentOrSalaryChangeStoresOutboxEvent() {
        Employee existingEmployee = employeeForUpdate();
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee);
        when(departmentMapper.findById(3L)).thenReturn(department(3L, "HR", true));

        employeeService.updateByHr(1L, updateRequest(3L, BigDecimal.valueOf(1500)));

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAggregateType()).isEqualTo("EMPLOYEE");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("employee.status.changed");
        assertThat(eventCaptor.getValue().getAggregateId()).isEqualTo(1L);
    }

    @Test
    void updateByHrRejectsEmailAlreadyInUse() {
        when(employeeMapper.findById(1L)).thenReturn(employeeForUpdate());
        Employee otherEmployee = new Employee();
        otherEmployee.setId(2L);
        when(employeeMapper.findByEmail("john@example.com")).thenReturn(otherEmployee);

        assertThatThrownBy(() -> employeeService.updateByHr(1L, updateRequest(2L, BigDecimal.valueOf(1000))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateByHrRejectsInactiveDepartment() {
        when(employeeMapper.findById(1L)).thenReturn(employeeForUpdate());
        when(departmentMapper.findById(2L)).thenReturn(department(2L, "IT", false));

        assertThatThrownBy(() -> employeeService.updateByHr(1L, updateRequest(2L, BigDecimal.valueOf(1000))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateContactUpdatesOnlyRequesterContactFields() {
        when(employeeMapper.findById(1L)).thenReturn(employeeForUpdate());
        when(departmentMapper.findById(2L)).thenReturn(department(2L, "IT", true));

        EmployeeResponse response = employeeService.updateContact(
                1L,
                new UpdateContactRequest("new@example.com", "987654321"),
                99L
        );

        verify(employeeMapper).updateContact(1L, "new@example.com", "987654321");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.phone()).isEqualTo("987654321");
        assertThat(response.departmentName()).isEqualTo("IT");
    }

    @Test
    void updateContactRejectsOtherRequester() {
        when(employeeMapper.findById(1L)).thenReturn(employeeForUpdate());

        assertThatThrownBy(() -> employeeService.updateContact(
                1L,
                new UpdateContactRequest("new@example.com", "987654321"),
                100L
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateContactRejectsEmailAlreadyInUse() {
        when(employeeMapper.findById(1L)).thenReturn(employeeForUpdate());
        Employee existing = new Employee();
        existing.setId(2L);
        when(employeeMapper.findByEmail("new@example.com")).thenReturn(existing);

        assertThatThrownBy(() -> employeeService.updateContact(
                1L,
                new UpdateContactRequest("new@example.com", "987654321"),
                99L
        )).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deactivateActiveEmployeeWithoutPendingLeavesStoresOutboxEvent() {
        when(employeeMapper.findById(1L)).thenReturn(employeeForDeactivate(EmployeeStatus.ACTIVE));
        when(leaveServiceClient.hasPendingLeavesByEmployeeId(1L)).thenReturn(false);

        employeeService.deactivate(1L);

        verify(employeeMapper).deactivate(1L);
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAggregateType()).isEqualTo("EMPLOYEE");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("employee.deactivated");
        assertThat(eventCaptor.getValue().getAggregateId()).isEqualTo(1L);
    }

    @Test
    void deactivateRejectsInactiveEmployee() {
        when(employeeMapper.findById(1L)).thenReturn(employeeForDeactivate(EmployeeStatus.INACTIVE));

        assertThatThrownBy(() -> employeeService.deactivate(1L)).isInstanceOf(BusinessException.class);
        verify(employeeMapper, never()).deactivate(1L);
    }

    @Test
    void deactivateRejectsEmployeeWithPendingLeaves() {
        when(employeeMapper.findById(1L)).thenReturn(employeeForDeactivate(EmployeeStatus.ACTIVE));
        when(leaveServiceClient.hasPendingLeavesByEmployeeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.deactivate(1L)).isInstanceOf(BusinessException.class);
        verify(employeeMapper, never()).deactivate(1L);
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

    private UpdateEmployeeRequest updateRequest(Long departmentId, BigDecimal salary) {
        return new UpdateEmployeeRequest("John Doe", "john@example.com", "123", "Dev", departmentId, salary, LocalDate.now());
    }

    private Department activeDepartment() {
        return department(1L, "Engineering", true);
    }

    private Department department(Long id, String name, boolean active) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setIsActive(active);
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

    private Employee employeeForUpdate() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setDepartmentId(2L);
        employee.setSalary(BigDecimal.valueOf(1000));
        employee.setAuthUserId(99L);
        return employee;
    }

    private Employee employeeForDeactivate(EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setStatus(status);
        return employee;
    }
}
