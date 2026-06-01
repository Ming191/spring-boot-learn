package vn.amela.leaveservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import vn.amela.leaveservice.dto.response.EmployeeSnapshotResponse;
import vn.amela.leaveservice.dto.response.LeaveResponse;
import vn.amela.leaveservice.entity.LeaveRequest;
import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;
import vn.amela.leaveservice.exception.ForbiddenActionException;
import vn.amela.leaveservice.exception.ResourceNotFoundException;
import vn.amela.leaveservice.mapper.LeaveMapper;
import vn.amela.leaveservice.mapper.OutboxEventMapper;
import vn.amela.leaveservice.security.CurrentUser;
import vn.amela.leaveservice.service.impl.LeaveServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock
    private LeaveMapper leaveMapper;

    @Mock
    private OutboxEventMapper outboxEventMapper;

    @Mock
    private EmployeeSnapshotService employeeSnapshotService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    @Test
    @DisplayName("getById allows HR to view any leave")
    void getByIdAllowsHrToViewAnyLeave() {
        LeaveRequest leaveRequest = leaveRequest(99L);
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(leaveRequest));

        LeaveResponse response = leaveService.getById(100L, hrUser());

        assertMappedResponse(response, 99L);
        verify(leaveMapper).findById(100L);
        verifyNoInteractions(employeeSnapshotService, outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById allows employee to view own leave")
    void getByIdAllowsEmployeeToViewOwnLeave() {
        LeaveRequest leaveRequest = leaveRequest(10L);
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(leaveRequest));
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employeeSnapshot());

        LeaveResponse response = leaveService.getById(100L, employeeUser());

        assertMappedResponse(response, 10L);
        verify(leaveMapper).findById(100L);
        verify(employeeSnapshotService).getEmployeeSnapshotByAuthUserId(2L);
        verifyNoInteractions(outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById rejects employee viewing another employee leave")
    void getByIdRejectsEmployeeViewingOtherLeave() {
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(leaveRequest(99L)));
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employeeSnapshot());

        assertThatThrownBy(() -> leaveService.getById(100L, employeeUser()))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("You can only view your own leave requests");

        verify(leaveMapper).findById(100L);
        verify(employeeSnapshotService).getEmployeeSnapshotByAuthUserId(2L);
        verifyNoInteractions(outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById rejects null user and does not call dependencies")
    void getByIdRejectsNullUser() {
        assertThatThrownBy(() -> leaveService.getById(100L, null))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR or Employee can view leave requests");

        verifyNoInteractions(leaveMapper, employeeSnapshotService, outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById rejects unsupported role and does not call dependencies")
    void getByIdRejectsUnsupportedRole() {
        assertThatThrownBy(() -> leaveService.getById(100L, new CurrentUser("manager", 3L, "MANAGER")))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR or Employee can view leave requests");

        verifyNoInteractions(leaveMapper, employeeSnapshotService, outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById rejects missing leave")
    void getByIdRejectsMissingLeave() {
        when(leaveMapper.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.getById(100L, employeeUser()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Leave request not found");

        verify(leaveMapper).findById(100L);
        verifyNoInteractions(employeeSnapshotService, outboxEventMapper, objectMapper);
    }

    private CurrentUser hrUser() {
        return new CurrentUser("hr_user", 1L, "HR");
    }

    private CurrentUser employeeUser() {
        return new CurrentUser("employee_user", 2L, "EMPLOYEE");
    }

    private EmployeeSnapshotResponse employeeSnapshot() {
        return EmployeeSnapshotResponse.builder()
                .id(10L)
                .employeeCode("EMP001")
                .fullName("Employee One")
                .email("employee.one@company.com")
                .phone("0123456789")
                .position("Developer")
                .status("ACTIVE")
                .authUserId(2L)
                .departmentId(30L)
                .departmentName("Engineering")
                .build();
    }

    private LeaveRequest leaveRequest(Long employeeId) {
        return LeaveRequest.builder()
                .id(100L)
                .employeeId(employeeId)
                .employeeCode("EMP001")
                .employeeName("Employee One")
                .departmentName("Engineering")
                .leaveType(LeaveType.ANNUAL)
                .fromDate(LocalDate.of(2026, 6, 10))
                .toDate(LocalDate.of(2026, 6, 12))
                .totalDays(3)
                .reason("Vacation")
                .status(LeaveStatus.PENDING)
                .reviewedBy(20L)
                .reviewerNote("note")
                .reviewedAt(LocalDateTime.of(2026, 6, 1, 9, 30))
                .createdAt(LocalDateTime.of(2026, 5, 1, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 2, 8, 0))
                .build();
    }

    private void assertMappedResponse(LeaveResponse response, Long employeeId) {
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.employeeName()).isEqualTo("Employee One");
        assertThat(response.departmentName()).isEqualTo("Engineering");
        assertThat(response.leaveType()).isEqualTo(LeaveType.ANNUAL);
        assertThat(response.fromDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.toDate()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(response.totalDays()).isEqualTo(3);
        assertThat(response.reason()).isEqualTo("Vacation");
        assertThat(response.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(response.reviewedBy()).isEqualTo(20L);
        assertThat(response.reviewerNote()).isEqualTo("note");
        assertThat(response.reviewedAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 30));
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 8, 0));
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 8, 0));
    }
}
