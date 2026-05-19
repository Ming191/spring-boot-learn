package vn.amela.employeeservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.DuplicateResourceException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.mapper.OutboxEventMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private OutboxEventMapper outboxEventMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void testUpdateByHr_Success_WithoutStatusChange() {
        Employee existingEmployee = new Employee();
        existingEmployee.setId(1L);
        existingEmployee.setDepartmentId(2L);
        existingEmployee.setSalary(BigDecimal.valueOf(1000));
        existingEmployee.setAuthUserId(99L);
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee);

        Department dept = new Department();
        dept.setId(2L);
        dept.setIsActive(true);
        dept.setName("IT");
        when(departmentMapper.findById(2L)).thenReturn(dept);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "John Doe",
                "john@example.com",
                "123",
                "Dev",
                2L,
                BigDecimal.valueOf(1000),
                LocalDate.now()
        );

        var response = employeeService.updateByHr(1L, request);

        verify(employeeMapper).updateByHr(any(Employee.class));
        verify(outboxEventMapper, never()).insert(any(OutboxEvent.class));
        assertEquals(99L, existingEmployee.getAuthUserId());
        assertNotNull(response);
    }

    @Test
    void testUpdateByHr_Success_WithStatusChange() throws Exception {
        Employee existingEmployee = new Employee();
        existingEmployee.setId(1L);
        existingEmployee.setDepartmentId(2L);
        existingEmployee.setSalary(BigDecimal.valueOf(1000));
        existingEmployee.setAuthUserId(99L);
        when(employeeMapper.findById(1L)).thenReturn(existingEmployee);

        Department dept = new Department();
        dept.setId(3L);
        dept.setIsActive(true);
        dept.setName("HR");
        when(departmentMapper.findById(3L)).thenReturn(dept);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "John Doe",
                "john@example.com",
                "123",
                "Dev",
                3L,
                BigDecimal.valueOf(1500),
                LocalDate.now()
        );

        when(objectMapper.writeValueAsString(any(Employee.class))).thenReturn("{}");

        employeeService.updateByHr(1L, request);

        verify(employeeMapper).updateByHr(any(Employee.class));
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(eventCaptor.capture());
        
        OutboxEvent event = eventCaptor.getValue();
        assertEquals("EMPLOYEE", event.getAggregateType());
        assertEquals("employee.status.changed", event.getEventType());
        assertEquals(99L, existingEmployee.getAuthUserId());
        assertEquals(3L, existingEmployee.getDepartmentId());
        assertEquals(BigDecimal.valueOf(1500), existingEmployee.getSalary());
    }

    @Test
    void testUpdateByHr_EmailAlreadyInUse() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);

        Employee otherEmployee = new Employee();
        otherEmployee.setId(2L);
        when(employeeMapper.findByEmail("john@example.com")).thenReturn(otherEmployee);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "John Doe",
                "john@example.com",
                "123",
                "Dev",
                2L,
                BigDecimal.valueOf(1000),
                LocalDate.now()
        );

        assertThrows(DuplicateResourceException.class, () -> employeeService.updateByHr(1L, request));
    }

    @Test
    void testUpdateByHr_DepartmentNotActive() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);

        Department dept = new Department();
        dept.setId(2L);
        dept.setIsActive(false);
        when(departmentMapper.findById(2L)).thenReturn(dept);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "John Doe",
                "john@example.com",
                "123",
                "Dev",
                2L,
                BigDecimal.valueOf(1000),
                LocalDate.now()
        );

        assertThrows(ResourceNotFoundException.class, () -> employeeService.updateByHr(1L, request));
    }

    @Test
    void testUpdateContact_Success() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        currentEmployee.setAuthUserId(99L);
        currentEmployee.setDepartmentId(2L);
        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);

        Department dept = new Department();
        dept.setId(2L);
        dept.setName("IT");
        when(departmentMapper.findById(2L)).thenReturn(dept);

        UpdateContactRequest request = new UpdateContactRequest("new@example.com", "987654321");
        
        var response = employeeService.updateContact(1L, request, 99L);

        verify(employeeMapper).updateContact(1L, "new@example.com", "987654321");
        assertNotNull(response);
        assertEquals("new@example.com", response.email());
        assertEquals("987654321", response.phone());
        assertEquals("IT", response.departmentName());
    }

    @Test
    void testUpdateContact_Forbidden_Throws() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        currentEmployee.setAuthUserId(99L);
        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);

        UpdateContactRequest request = new UpdateContactRequest("new@example.com", "987654321");

        assertThrows(BusinessException.class, () -> employeeService.updateContact(1L, request, 100L));
    }

    @Test
    void testUpdateContact_EmailAlreadyInUse() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        currentEmployee.setAuthUserId(99L);
        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);

        Employee existing = new Employee();
        existing.setId(2L);
        when(employeeMapper.findByEmail("new@example.com")).thenReturn(existing);

        UpdateContactRequest request = new UpdateContactRequest("new@example.com", "987654321");

        assertThrows(DuplicateResourceException.class, () -> employeeService.updateContact(1L, request, 99L));
    }
}
