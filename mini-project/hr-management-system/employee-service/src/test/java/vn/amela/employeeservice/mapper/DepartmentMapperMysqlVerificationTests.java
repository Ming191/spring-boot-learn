package vn.amela.employeeservice.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.employeeservice.entity.Department;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "verify.mysql", matches = "true")
class DepartmentMapperMysqlVerificationTests {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Test
    void verifiesDepartmentMapperAgainstComposeMysql() {
        Department engineering = departmentMapper.findByName("Engineering");
        assertThat(engineering).isNotNull();
        assertThat(engineering.getId()).isEqualTo(1L);
        assertThat(engineering.getManagerId()).isEqualTo(1L);
        assertThat(engineering.getIsActive()).isTrue();

        assertThat(departmentMapper.findById(engineering.getId()).getName()).isEqualTo("Engineering");
        assertThat(departmentMapper.findAll())
                .extracting(Department::getName)
                .contains("Engineering", "HR", "Marketing", "Finance");
        assertThat(departmentMapper.findActive())
                .extracting(Department::getName)
                .contains("Engineering", "HR", "Marketing", "Finance");

        assertThat(departmentMapper.existsByNameExceptId("Engineering", engineering.getId())).isFalse();
        assertThat(departmentMapper.existsByNameExceptId("Engineering", 999L)).isTrue();

        Department newDepartment = new Department();
        newDepartment.setName("Verification");
        newDepartment.setDescription("Mapper verification department");
        newDepartment.setManagerId(null);
        newDepartment.setIsActive(true);

        departmentMapper.insert(newDepartment);
        assertThat(newDepartment.getId()).isNotNull();
        Department inserted = departmentMapper.findById(newDepartment.getId());
        assertThat(inserted.getName()).isEqualTo("Verification");
        assertThat(inserted.getIsActive()).isTrue();

        inserted.setName("Verification Updated");
        inserted.setDescription("Updated by mapper verification");
        inserted.setManagerId(1L);
        inserted.setIsActive(true);
        assertThat(departmentMapper.update(inserted)).isEqualTo(1);

        Department updated = departmentMapper.findById(inserted.getId());
        assertThat(updated.getName()).isEqualTo("Verification Updated");
        assertThat(updated.getDescription()).isEqualTo("Updated by mapper verification");
        assertThat(updated.getManagerId()).isEqualTo(1L);
        assertThat(updated.getIsActive()).isTrue();

        assertThat(departmentMapper.deactivate(updated.getId())).isEqualTo(1);
        assertThat(departmentMapper.findById(updated.getId()).getIsActive()).isFalse();
    }
}
