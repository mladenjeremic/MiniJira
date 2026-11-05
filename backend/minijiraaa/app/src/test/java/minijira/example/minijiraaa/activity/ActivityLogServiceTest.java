package minijira.example.minijiraaa.activity;

import minijira.example.minijiraaa.application.dto.ActivityResponse;
import minijira.example.minijiraaa.application.service.ActivityLogService;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.model.ActivityLog;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.ActivityLogStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import minijira.example.minijiraaa.application.mapper.UserMapper;
import minijira.example.minijiraaa.application.mapper.UserMapperImpl;
import minijira.example.minijiraaa.application.mapper.ActivityMapper;
import minijira.example.minijiraaa.application.mapper.ActivityMapperImpl;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock ActivityLogStore activityLogStore;

    @Spy UserMapper userMapper = new UserMapperImpl();
    @Spy ActivityMapper activityMapper = new ActivityMapperImpl(userMapper);

    @InjectMocks ActivityLogService activityLogService;

    private static User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    @Test
    void log_savesEntryWithAllFields() {
        Project project = new Project();
        project.setId(1L);
        User actor = user(2L, "Ana");

        activityLogService.log(project, actor, ActivityType.TASK_UPDATED,
                "MJ-1", "status", "Todo", "Done");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogStore).save(captor.capture());
        ActivityLog saved = captor.getValue();

        assertThat(saved.getProject()).isSameAs(project);
        assertThat(saved.getActor()).isSameAs(actor);
        assertThat(saved.getType()).isEqualTo(ActivityType.TASK_UPDATED);
        assertThat(saved.getTaskKey()).isEqualTo("MJ-1");
        assertThat(saved.getFieldName()).isEqualTo("status");
        assertThat(saved.getOldValue()).isEqualTo("Todo");
        assertThat(saved.getNewValue()).isEqualTo("Done");
    }

    @Test
    void log_cutsValuesLongerThan500Characters_andKeepsNull() {
        activityLogService.log(new Project(), user(1L, "Ana"), ActivityType.TASK_UPDATED,
                "MJ-1", "title", "x".repeat(600), null);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogStore).save(captor.capture());

        assertThat(captor.getValue().getOldValue()).hasSize(500);
        assertThat(captor.getValue().getNewValue()).isNull();
    }

    @Test
    void getForProject_buildsReadableMessages() {
        ActivityLog created = new ActivityLog();
        created.setType(ActivityType.TASK_CREATED);
        created.setActor(user(1L, "Ana"));
        created.setTaskKey("MJ-1");

        ActivityLog changed = new ActivityLog();
        changed.setType(ActivityType.TASK_UPDATED);
        changed.setActor(user(1L, "Ana"));
        changed.setTaskKey("MJ-1");
        changed.setFieldName("status");
        changed.setOldValue("Todo");
        changed.setNewValue("Done");

        ActivityLog noActor = new ActivityLog();
        noActor.setType(ActivityType.PROJECT_CREATED);

        when(activityLogStore.findLatestByProjectId(10L))
                .thenReturn(List.of(created, changed, noActor));

        List<ActivityResponse> result = activityLogService.getForProject(10L);

        assertThat(result).extracting(ActivityResponse::message).containsExactly(
                "Ana created MJ-1",
                "Ana changed the status of MJ-1 from Todo to Done",
                "Someone created the project");
    }
}