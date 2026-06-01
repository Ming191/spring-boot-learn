package vn.amela.leaveservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import vn.amela.leaveservice.dto.request.ReviewLeaveRequest;
import vn.amela.leaveservice.dto.response.LeaveResponse;
import vn.amela.leaveservice.entity.LeaveRequest;
import vn.amela.leaveservice.entity.OutboxEvent;
import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;
import vn.amela.leaveservice.exception.BusinessException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    @DisplayName("approve updates pending leave and returns reloaded response")
    void approveUpdatesPendingLeaveAndReturnsReloadedResponse() throws Exception {
        LeaveRequest pendingLeave = leaveRequest(LeaveStatus.PENDING, null, null, null);
        LeaveRequest approvedLeave = leaveRequest(
                LeaveStatus.APPROVED,
                1L,
                "Approved",
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );
        ReviewLeaveRequest request = ReviewLeaveRequest.builder()
                .reviewerNote("Approved")
                .build();
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(pendingLeave), Optional.of(approvedLeave));
        when(leaveMapper.approve(any(), any(), any(), any())).thenReturn(1);
        when(objectMapper.writeValueAsString(approvedLeave)).thenReturn("{}");

        LeaveResponse response = leaveService.approve(100L, request, hrUser());

        assertMappedResponse(response, LeaveStatus.APPROVED, 1L, "Approved");
        ArgumentCaptor<LocalDateTime> reviewedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(leaveMapper).approve(eq(100L), eq(1L), eq("Approved"), reviewedAtCaptor.capture());
        assertThat(reviewedAtCaptor.getValue()).isNotNull();
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAggregateType()).isEqualTo("LEAVE_REQUEST");
        assertThat(eventCaptor.getValue().getAggregateId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("leave.approved");
        assertThat(eventCaptor.getValue().getPayload()).isEqualTo("{}");
        verifyNoInteractions(employeeSnapshotService);
    }

    @Test
    @DisplayName("approve allows null request with null reviewer note")
    void approveAllowsNullRequestWithNullReviewerNote() throws Exception {
        LeaveRequest pendingLeave = leaveRequest(LeaveStatus.PENDING, null, null, null);
        LeaveRequest approvedLeave = leaveRequest(LeaveStatus.APPROVED, 1L, null, LocalDateTime.of(2026, 6, 1, 10, 0));
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(pendingLeave), Optional.of(approvedLeave));
        when(leaveMapper.approve(any(), any(), any(), any())).thenReturn(1);
        when(objectMapper.writeValueAsString(approvedLeave)).thenReturn("{}");

        leaveService.approve(100L, null, hrUser());

        verify(leaveMapper).approve(any(), any(), org.mockito.ArgumentMatchers.isNull(), any());
        verify(outboxEventMapper).insert(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("approve rejects null user and does not mutate")
    void approveRejectsNullUser() {
        assertThatThrownBy(() -> leaveService.approve(100L, ReviewLeaveRequest.builder().build(), null))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR can review leave requests");

        verifyNoInteractions(leaveMapper, outboxEventMapper, employeeSnapshotService, objectMapper);
    }

    @Test
    @DisplayName("approve rejects non HR user and does not mutate")
    void approveRejectsNonHrUser() {
        assertThatThrownBy(() -> leaveService.approve(100L, ReviewLeaveRequest.builder().build(), employeeUser()))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR can review leave requests");

        verifyNoInteractions(leaveMapper, outboxEventMapper, employeeSnapshotService, objectMapper);
    }

    @Test
    @DisplayName("approve rejects missing leave")
    void approveRejectsMissingLeave() {
        when(leaveMapper.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.approve(100L, ReviewLeaveRequest.builder().build(), hrUser()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Leave request not found");

        verify(leaveMapper).findById(100L);
        verify(leaveMapper, never()).approve(any(), any(), any(), any());
        verifyNoInteractions(outboxEventMapper, employeeSnapshotService, objectMapper);
    }

    @Test
    @DisplayName("approve rejects non pending leave")
    void approveRejectsNonPendingLeave() {
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(leaveRequest(LeaveStatus.APPROVED, 1L, "Approved", LocalDateTime.now())));
        when(leaveMapper.approve(any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> leaveService.approve(100L, ReviewLeaveRequest.builder().reviewerNote("Approved").build(), hrUser()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only pending leave requests can be approved");

        verify(leaveMapper).findById(100L);
        verify(leaveMapper).approve(any(), any(), any(), any());
        verifyNoInteractions(outboxEventMapper, employeeSnapshotService, objectMapper);
    }

    private CurrentUser hrUser() {
        return new CurrentUser("hr_user", 1L, "HR");
    }

    private CurrentUser employeeUser() {
        return new CurrentUser("employee_user", 2L, "EMPLOYEE");
    }

    private LeaveRequest leaveRequest(
            LeaveStatus status,
            Long reviewedBy,
            String reviewerNote,
            LocalDateTime reviewedAt
    ) {
        return LeaveRequest.builder()
                .id(100L)
                .employeeId(10L)
                .employeeCode("EMP001")
                .employeeName("Employee One")
                .departmentName("Engineering")
                .leaveType(LeaveType.ANNUAL)
                .fromDate(LocalDate.of(2026, 6, 10))
                .toDate(LocalDate.of(2026, 6, 12))
                .totalDays(3)
                .reason("Vacation")
                .status(status)
                .reviewedBy(reviewedBy)
                .reviewerNote(reviewerNote)
                .reviewedAt(reviewedAt)
                .createdAt(LocalDateTime.of(2026, 5, 1, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 2, 8, 0))
                .build();
    }

    private void assertMappedResponse(
            LeaveResponse response,
            LeaveStatus status,
            Long reviewedBy,
            String reviewerNote
    ) {
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.employeeId()).isEqualTo(10L);
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.employeeName()).isEqualTo("Employee One");
        assertThat(response.departmentName()).isEqualTo("Engineering");
        assertThat(response.leaveType()).isEqualTo(LeaveType.ANNUAL);
        assertThat(response.fromDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.toDate()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(response.totalDays()).isEqualTo(3);
        assertThat(response.reason()).isEqualTo("Vacation");
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.reviewedBy()).isEqualTo(reviewedBy);
        assertThat(response.reviewerNote()).isEqualTo(reviewerNote);
        assertThat(response.reviewedAt()).isNotNull();
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 8, 0));
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 8, 0));
    }
}
