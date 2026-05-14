package vn.amela.employeeservice.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "verify.mysql", matches = "true")
class EmployeeMapperMysqlVerificationTests {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Test
    void verifiesEmployeeMapperAgainstComposeMysql() {
        Employee emp001 = employeeMapper.findByEmployeeCode("EMP001");
        assertThat(emp001).isNotNull();
        assertThat(emp001.getEmail()).isEqualTo("nguyen@company.com");

        assertThat(employeeMapper.findByEmail("nguyen@company.com").getId()).isEqualTo(emp001.getId());
        assertThat(employeeMapper.findByAuthUserId(3L).getEmployeeCode()).isEqualTo("EMP001");

        List<Employee> engineeringEmployees = employeeMapper.search(
                null,
                1L,
                null,
                EmployeeStatus.ACTIVE,
                null,
                null,
                "employeeCode",
                "asc",
                0,
                10
        );
        assertThat(engineeringEmployees)
                .extracting(Employee::getEmployeeCode)
                .containsExactly("EMP001", "EMP002");
        assertThat(employeeMapper.countByFilter(null, 1L, null, EmployeeStatus.ACTIVE, null, null)).isEqualTo(2);

        assertThat(employeeMapper.countActiveByDepartmentId(1L)).isEqualTo(2);
        assertThat(employeeMapper.existsActiveInDepartment(1L, 1L)).isTrue();
        assertThat(employeeMapper.existsActiveInDepartment(3L, 1L)).isFalse();

        Employee newEmployee = new Employee();
        newEmployee.setEmployeeCode("EMP999");
        newEmployee.setFullName("Mapper Verification");
        newEmployee.setEmail("mapper.verify@company.com");
        newEmployee.setPhone("0987654321");
        newEmployee.setPosition("QA Engineer");
        newEmployee.setDepartmentId(1L);
        newEmployee.setAuthUserId(999L);
        newEmployee.setSalary(new BigDecimal("11000000.00"));
        newEmployee.setStartDate(LocalDate.of(2026, 5, 14));
        newEmployee.setStatus(EmployeeStatus.ACTIVE);

        employeeMapper.insert(newEmployee);
        assertThat(newEmployee.getId()).isNotNull();
        assertThat(employeeMapper.findById(newEmployee.getId()).getEmployeeCode()).isEqualTo("EMP999");

        assertThat(employeeMapper.existsByEmployeeCodeExceptId("EMP001", emp001.getId())).isFalse();
        assertThat(employeeMapper.existsByEmployeeCodeExceptId("EMP001", 999L)).isTrue();
        assertThat(employeeMapper.existsByEmailExceptId("nguyen@company.com", emp001.getId())).isFalse();
        assertThat(employeeMapper.existsByEmailExceptId("nguyen@company.com", 999L)).isTrue();

        Employee emp004 = employeeMapper.findByEmployeeCode("EMP004");
        Long originalAuthUserId = emp004.getAuthUserId();
        emp004.setAuthUserId(999L);
        emp004.setSalary(new BigDecimal("13500000.00"));
        assertThat(employeeMapper.updateByHr(emp004)).isEqualTo(1);
        Employee updatedEmp004 = employeeMapper.findById(emp004.getId());
        assertThat(updatedEmp004.getAuthUserId()).isEqualTo(originalAuthUserId);
        assertThat(updatedEmp004.getSalary()).isEqualByComparingTo("13500000.00");

        assertThat(employeeMapper.updateContact(emp004.getId(), "verify.emp004@company.com", "0999999999")).isEqualTo(1);
        Employee contactUpdated = employeeMapper.findById(emp004.getId());
        assertThat(contactUpdated.getEmail()).isEqualTo("verify.emp004@company.com");
        assertThat(contactUpdated.getPhone()).isEqualTo("0999999999");

        assertThat(employeeMapper.deactivate(emp004.getId())).isEqualTo(1);
        assertThat(employeeMapper.findById(emp004.getId()).getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
    }
}
