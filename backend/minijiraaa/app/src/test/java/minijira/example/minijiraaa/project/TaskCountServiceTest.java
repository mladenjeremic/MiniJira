package minijira.example.minijiraaa.project;

import minijira.example.minijiraaa.application.service.TaskCountService;
import minijira.example.minijiraaa.domain.store.TaskCountStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCountServiceTest {

    @Mock TaskCountStore taskCountStore;

    @InjectMocks TaskCountService taskCountService;

    @Test
    void countByProjectId_delegatesToStore() {
        when(taskCountStore.countByProjectId(10L)).thenReturn(7L);

        assertThat(taskCountService.countByProjectId(10L)).isEqualTo(7L);
    }
}