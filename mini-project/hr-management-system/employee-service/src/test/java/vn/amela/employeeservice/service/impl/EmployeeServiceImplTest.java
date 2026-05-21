package vn.amela.employeeservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.amela.employeeservice.client.LeaveServiceClient;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.mapper.OutboxEventMapper;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private LeaveServiceClient leaveServiceClient;

    @Mock
    private OutboxEventMapper outboxEventMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void testDeactivate_Success() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        currentEmployee.setStatus(EmployeeStatus.ACTIVE);

        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);
        when(leaveServiceClient.hasPendingLeavesByEmployeeId(1L)).thenReturn(false);
        when(employeeMapper.deactivate(1L)).thenReturn(1);
        when(objectMapper.writeValueAsString(currentEmployee)).thenReturn("{}");

        employeeService.deactivate(1L);

        verify(employeeMapper).deactivate(1L);
        
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(eventCaptor.capture());
        OutboxEvent event = eventCaptor.getValue();
        assertEquals("Employee", event.getAggregateType());
        assertEquals("employee.deactivated", event.getEventType());
        assertEquals(1L, event.getAggregateId());
    }

    @Test
    void testDeactivate_EmployeeNotActive() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        currentEmployee.setStatus(EmployeeStatus.INACTIVE);
        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);

        assertThrows(BusinessException.class, () -> employeeService.deactivate(1L));
        verify(employeeMapper, never()).deactivate(1L);
    }

    @Test
    void testDeactivate_HasPendingLeaves() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        currentEmployee.setStatus(EmployeeStatus.ACTIVE);
        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);
        
        when(leaveServiceClient.hasPendingLeavesByEmployeeId(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> employeeService.deactivate(1L));
        verify(employeeMapper, never()).deactivate(1L);
    }

    @Test
    void testDeactivate_UpdateCountZero_Throws() {
        Employee currentEmployee = new Employee();
        currentEmployee.setId(1L);
        currentEmployee.setStatus(EmployeeStatus.ACTIVE);

        when(employeeMapper.findById(1L)).thenReturn(currentEmployee);
        when(leaveServiceClient.hasPendingLeavesByEmployeeId(1L)).thenReturn(false);
        when(employeeMapper.deactivate(1L)).thenReturn(0);

        assertThrows(BusinessException.class, () -> employeeService.deactivate(1L));

        verify(outboxEventMapper, never()).insert(any());
    }
}
