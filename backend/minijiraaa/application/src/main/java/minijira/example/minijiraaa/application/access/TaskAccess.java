package minijira.example.minijiraaa.application.access;

import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.model.User;

public interface TaskAccess {

    /** Vraća task samo ako korisnik ima pristup njegovom projektu. */
    Task getEntityForUser(Long id, User user);
}