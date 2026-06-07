package vn.amela.leaveservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.amela.leaveservice.dto.request.CreateLeaveRequest;
import vn.amela.leaveservice.dto.request.LeaveFilterRequest;
import vn.amela.leaveservice.dto.request.RejectLeaveRequest;
import vn.amela.leaveservice.dto.request.ReviewLeaveRequest;
import vn.amela.leaveservice.dto.response.LeaveResponse;
import vn.amela.leaveservice.dto.response.PageResponse;
import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;
import vn.amela.leaveservice.exception.BusinessException;
import vn.amela.leaveservice.security.CurrentUser;
import vn.amela.leaveservice.security.CurrentUserProvider;
import vn.amela.leaveservice.service.LeaveService;

import java.time.LocalDate;

@Controller
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeavePageController {

    private final LeaveService leaveService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public String list(@RequestParam(required = false) Long employeeId,
                       @RequestParam(required = false) LeaveStatus status,
                       @RequestParam(required = false) LeaveType leaveType,
                       @RequestParam(required = false) LocalDate fromDate,
                       @RequestParam(required = false) LocalDate toDate,
                       @RequestParam(required = false) String departmentName,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       HttpServletRequest request,
                       Model model) {
        CurrentUser user = currentUserProvider.getCurrentUser(request);
        LeaveFilterRequest filter = LeaveFilterRequest.builder()
                .employeeId(employeeId)
                .status(status)
                .leaveType(leaveType)
                .fromDate(fromDate)
                .toDate(toDate)
                .departmentName(departmentName)
                .page(page)
                .size(size)
                .sortBy("createdAt")
                .sortDirection("desc")
                .build();
        PageResponse<LeaveResponse> leaves = leaveService.search(filter, user);
        addSharedModel(model, user);
        model.addAttribute("leaves", leaves);
        model.addAttribute("filter", filter);
        model.addAttribute("myView", false);
        return "leave/list";
    }

    @GetMapping("/my")
    public String myLeaves(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           HttpServletRequest request,
                           Model model) {
        CurrentUser user = currentUserProvider.getCurrentUser(request);
        if (user.isHr()) {
            return "redirect:/leaves";
        }
        PageResponse<LeaveResponse> leaves = leaveService.findMyLeaves(user, page, size);
        addSharedModel(model, user);
        model.addAttribute("leaves", leaves);
        model.addAttribute("filter", LeaveFilterRequest.builder().page(page).size(size).build());
        model.addAttribute("myView", true);
        return "leave/my";
    }

    @GetMapping("/new")
    public String newForm(HttpServletRequest request, Model model) {
        addSharedModel(model, currentUserProvider.getCurrentUser(request));
        if (!model.containsAttribute("leaveForm")) {
            model.addAttribute("leaveForm", LeaveForm.empty());
        }
        model.addAttribute("today", LocalDate.now());
        return "leave/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("leaveForm") LeaveForm form,
                         BindingResult bindingResult,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        CurrentUser user = currentUserProvider.getCurrentUser(request);
        rejectInvalidDateRange(form, bindingResult);
        if (bindingResult.hasErrors()) {
            addSharedModel(model, user);
            model.addAttribute("today", LocalDate.now());
            return "leave/form";
        }
        try {
            LeaveResponse created = leaveService.create(new CreateLeaveRequest(
                    form.leaveType(), form.fromDate(), form.toDate(), form.reason()), user);
            redirectAttributes.addFlashAttribute("success", "Leave request created.");
            return "redirect:/leaves/" + created.id();
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            addSharedModel(model, user);
            model.addAttribute("today", LocalDate.now());
            return "leave/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        CurrentUser user = currentUserProvider.getCurrentUser(request);
        addSharedModel(model, user);
        LeaveResponse leave = leaveService.getById(id, user);
        model.addAttribute("leave", leave);
        model.addAttribute("canCancel", user.isEmployee() && LeaveStatus.PENDING.equals(leave.status()));
        return "leave/detail";
    }

    @GetMapping("/{id}/review")
    public String review(@PathVariable Long id, HttpServletRequest request, Model model) {
        CurrentUser user = currentUserProvider.getCurrentUser(request);
        addSharedModel(model, user);
        model.addAttribute("leave", leaveService.getById(id, user));
        return "leave/review";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String reviewerNote,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            leaveService.approve(id, new ReviewLeaveRequest(reviewerNote), currentUserProvider.getCurrentUser(request));
            redirectAttributes.addFlashAttribute("success", "Leave request approved.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/leaves/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String reviewerNote,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            leaveService.reject(id, new RejectLeaveRequest(reviewerNote), currentUserProvider.getCurrentUser(request));
            redirectAttributes.addFlashAttribute("success", "Leave request rejected.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/leaves/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            leaveService.cancel(id, currentUserProvider.getCurrentUser(request));
            redirectAttributes.addFlashAttribute("success", "Leave request cancelled.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/leaves/" + id;
    }

    private void rejectInvalidDateRange(LeaveForm form, BindingResult bindingResult) {
        if (form == null || form.fromDate() == null || form.toDate() == null) {
            return;
        }
        if (form.fromDate().isAfter(form.toDate())) {
            bindingResult.rejectValue(
                    "toDate",
                    "dateRange",
                    "To date must be greater than or equal to from date"
            );
        }

        if (form.fromDate().isBefore(LocalDate.now())) {
            bindingResult.rejectValue(
                    "fromDate",
                    "pastDate",
                    "From date must not be in the past"
            );
        }
    }

    private void addSharedModel(Model model, CurrentUser user) {
        model.addAttribute("currentUser", user);
        model.addAttribute("leaveTypes", LeaveType.values());
        model.addAttribute("leaveStatuses", LeaveStatus.values());
        model.addAttribute("isHr", "HR".equalsIgnoreCase(user.role()));
    }

    public record LeaveForm(
            @NotNull(message = "Leave type cannot be null") LeaveType leaveType,
            @NotNull(message = "From date cannot be null") LocalDate fromDate,
            @NotNull(message = "To date cannot be null") LocalDate toDate,
            @NotBlank(message = "Reason cannot be blank") String reason
    ) {
        public static LeaveForm empty() {
            return new LeaveForm(null, null, null, "");
        }
    }
}
