package lcsfw.fw.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import jakarta.servlet.http.Part;
import lcsfw.fw.conf.AppProperties;

public class Mapfile {

    private String nom;
    private String extension;
    private String contentType;
    private long size;
    private byte[] contenu;

    public Mapfile() {
    }

    public static Mapfile fromPart(Part part) throws IOException {
        if (part == null) {
            return null;
        }

        Mapfile mapfile = new Mapfile();

        String submittedName = part.getSubmittedFileName();

        if (submittedName == null || submittedName.isBlank()) {
            return null;
        }

        String fullName = submittedName.replace('\\', '/');
        fullName = fullName.substring(fullName.lastIndexOf('/') + 1);

        int lastDot = fullName.lastIndexOf('.');

        if (lastDot > 0) {
            mapfile.nom = fullName.substring(0, lastDot);
            mapfile.extension = fullName.substring(lastDot + 1);
        } else {
            mapfile.nom = fullName;
            mapfile.extension = "";
        }

        mapfile.contentType = part.getContentType();
        mapfile.size = part.getSize();

        try (InputStream input = part.getInputStream()) {
            mapfile.contenu = input.readAllBytes();
        }

        return mapfile;
    }

    public String saveToUploadDir() throws IOException {
        return save(getUploaddir());
    }

    public String save(Path path) throws IOException {
        Path base = path.toAbsolutePath().normalize();
        Files.createDirectories(base);

        String ext = extension == null ? "" : extension.replaceAll("[^A-Za-z0-9]", "");
        String nomGenere = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        Path cible = base.resolve(nomGenere).normalize();
        if (!cible.startsWith(base)) {
            throw new IOException("Chemin de destination refusé : " + cible);
        }

        Files.write(cible, contenu == null ? new byte[0] : contenu);
        return nomGenere;
    }

    public static Path getChemin(String nomGenere) {
        Path base = getUploaddir().toAbsolutePath().normalize();
        Path chemin = base.resolve(nomGenere).normalize();
        if (!chemin.startsWith(base)) {
            throw new IllegalArgumentException("Nom de fichier refusé : " + nomGenere);
        }
        return chemin;
    }

    private static Path getUploaddir() {
        String dir = AppProperties.get("upload.dir");
        if (dir == null || dir.isBlank()) {
            throw new IllegalStateException("upload.dir n'est pas défini dans application.properties");
        }
        return Paths.get(dir);
    }

    public String getNomComplet() {
        return extension.isEmpty() ? nom : nom + "." + extension;
    }

    public String getNom() {
        return nom;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public byte[] getContenu() {
        return contenu;
    }
}