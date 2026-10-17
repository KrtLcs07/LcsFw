package lcsfw.fw.file;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.http.Part;

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