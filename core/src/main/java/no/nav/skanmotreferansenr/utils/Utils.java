package no.nav.skanmotreferansenr.utils;

public class Utils {

    public static String changeFiletypeInFilename(String originalName, String newFiletype) {
        return removeFileExtensionInFilename(originalName) + "." + newFiletype;
    }

    public static String removeFileExtensionInFilename(String originalName) {
        return originalName.substring(0, originalName.lastIndexOf("."));
    }
}