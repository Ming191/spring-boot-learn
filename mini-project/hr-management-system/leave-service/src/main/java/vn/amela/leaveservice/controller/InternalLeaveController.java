package vn.amela.leaveservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.amela.leaveservice.mapper.LeaveMapper;

@RestController
@RequestMapping("/internal/leaves")
@RequiredArgsConstructor
public class InternalLeaveController {

    private final LeaveMapper leaveMapper;

    @GetMapping("/pending-count")
    public int countPendingLeavesByEmployeeId(@RequestParam Long employeeId) {
        return leaveMapper.countPendingByEmployeeId(employeeId);
    }
}
