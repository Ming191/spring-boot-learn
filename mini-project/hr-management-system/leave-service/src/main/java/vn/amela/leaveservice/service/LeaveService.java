package vn.amela.leaveservice.service;

import vn.amela.leaveservice.dto.request.CreateLeaveRequest;
import vn.amela.leaveservice.dto.request.LeaveFilterRequest;
import vn.amela.leaveservice.dto.request.RejectLeaveRequest;
import vn.amela.leaveservice.dto.request.ReviewLeaveRequest;
import vn.amela.leaveservice.dto.response.LeaveResponse;
import vn.amela.leaveservice.dto.response.PageResponse;
import vn.amela.leaveservice.security.CurrentUser;

public interface LeaveService {

    LeaveResponse create(CreateLeaveRequest request, CurrentUser user);

    PageResponse<LeaveResponse> search(LeaveFilterRequest filter, CurrentUser user);

    PageResponse<LeaveResponse> findMyLeaves(CurrentUser user, int page, int size);

    LeaveResponse getById(Long id, CurrentUser user);

    LeaveResponse approve(Long id, ReviewLeaveRequest request, CurrentUser user);

    LeaveResponse reject(Long id, RejectLeaveRequest request, CurrentUser user);

    LeaveResponse cancel(Long id, CurrentUser user);
}