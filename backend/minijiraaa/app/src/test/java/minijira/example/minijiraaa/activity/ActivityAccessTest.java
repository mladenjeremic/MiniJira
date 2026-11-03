package minijira.example.minijiraaa.activity;

import minijira.example.minijiraaa.application.access.ActivityAccess;
import minijira.example.minijiraaa.domain.enums.Priority;
import minijira.example.minijiraaa.domain.enums.Status;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.Task;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Cista logika, bez mock-ova: najlaksi test za pocetak. */
class ActivityAccessTest {

    @Test
    void label_turnsEnumIntoReadableText() {
        assertThat(ActivityAccess.label(Status.IN_PROGRESS)).isEqualTo("In Progress");
        assertThat(ActivityAccess.label(Status.DONE)).isEqualTo("Done");
        assertThat(ActivityAccess.label(Priority.HIGH)).isEqualTo("High");
    }

    @Test
    void label_returnsNullForNull() {
        assertThat(ActivityAccess.label(null)).isNull();
    }

    @Test
    void keyOf_combinesProjectKeyAndTaskNumber() {
        Project project = new Project();
        project.setProjectKey("MJ");

        Task task = new Task();
        task.setProject(project);
        task.setTaskNumber(3);

        assertThat(ActivityAccess.keyOf(task)).isEqualTo("MJ-3");
    }

    @Test
    void keyOf_returnsNullForNullTask() {
        assertThat(ActivityAccess.keyOf(null)).isNull();
    }
}