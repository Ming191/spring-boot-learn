package vn.amela.leaveservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.amela.leaveservice.dto.request.CreateLeaveRequest;
import vn.amela.leaveservice.dto.request.LeaveFilterRequest;
import vn.amela.leaveservice.dto.request.RejectLeaveRequest;
import vn.amela.leaveservice.dto.request.ReviewLeaveRequest;
import vn.amela.leaveservice.dto.response.LeaveResponse;
import vn.amela.leaveservice.dto.response.PageResponse;
import vn.amela.leaveservice.security.CurrentUser;
import vn.amela.leaveservice.service.LeaveService;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {
    @Override
    public LeaveResponse create(CreateLeaveRequest request, CurrentUser user) {
        return null;
    }

    @Override
    public PageResponse<LeaveResponse> search(LeaveFilterRequest filter, CurrentUser user) {
        return null;
    }

    @Override
    public PageResponse<LeaveResponse> findMyLeaves(CurrentUser user, int page, int size) {
        return null;
    }

    @Override
    public LeaveResponse getById(Long id, CurrentUser user) {
        return null;
    }

    @Override
    public LeaveResponse approve(Long id, ReviewLeaveRequest request, CurrentUser user) {
        return null;
    }

    @Override
    public LeaveResponse reject(Long id, RejectLeaveRequest request, CurrentUser user) {
        return null;
    }

    @Override
    public LeaveResponse cancel(Long id, CurrentUser user) {
        return null;
    }
}
