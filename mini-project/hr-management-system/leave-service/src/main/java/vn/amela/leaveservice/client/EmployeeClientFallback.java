package vn.amela.leaveservice.client;

import org.springframework.stereotype.Component;
import vn.amela.leaveservice.dto.response.EmployeeSnapshotResponse;
import vn.amela.leaveservice.exception.BusinessException;

@Component
public class EmployeeClientFallback implements EmployeeClient {

    @Override
    public EmployeeSnapshotResponse findByAuthUserId(Long authUserId) {
        throw new BusinessException("Employee service is currently unavailable. Please try again later.");
    }
}
