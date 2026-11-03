package minijira.example.minijiraaa.domain.store;

public interface ImageStore {

    /** Čuva bajtove i vraća generisano ime fajla (npr. 3f2a...-.png). */
    String save(byte[] data, String extension);

    /** Vraća bajtove slike; baca NotFoundException ako fajl ne postoji. */
    byte[] load(String fileName);

    /** Briše fajl; ako ne postoji, ne radi ništa. */
    void delete(String fileName);
}