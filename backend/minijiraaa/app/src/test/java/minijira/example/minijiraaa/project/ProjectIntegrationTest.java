package minijira.example.minijiraaa.project;


import minijira.example.minijiraaa.domain.store.ProjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProjectIntegrationTest {

    @Autowired
    private ProjectStore projectStore;
}