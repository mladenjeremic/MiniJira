package minijira.example.minijiraaa.application.access;

import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.User;

public interface ProjectAccess {

    Project getEntityById(Long id);

    void assertAccess(Long projectId, User user);
}