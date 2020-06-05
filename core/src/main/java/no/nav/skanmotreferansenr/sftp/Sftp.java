package no.nav.skanmotreferansenr.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrSftpTechnicalException;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Component
public class Sftp {
    private String APPLICATION = "Skanmotreferansenr";

    private JSch jsch = new JSch();
    private Session jschSession;
    private ChannelSftp channelSftp;
    private String homePath;

    private String host;
    private String username;
    private String port;
    private String privateKey;
    private String hostKey;

    public Sftp(SkanmotreferansenrProperties properties) {
        this.host = properties.getSftp().getHost();
        this.username = properties.getSftp().getUsername();
        this.port = properties.getSftp().getPort();
        this.privateKey = properties.getSftp().getPrivateKey();
        this.hostKey = properties.getSftp().getHostKey();
    }

    public List<String> listFiles() {
        return listFiles("*");
    }

    public List<String> listFiles(String path) {
        connectIfNotConnected();
        try {
            Vector<LsEntry> vector = channelSftp.ls(path);
            return vector.stream().map(ChannelSftp.LsEntry::getFilename).collect(Collectors.toList());
        } catch (SftpException e) {
            log.error(APPLICATION + " failed to list files, path: " + path, e);
            throw new SkanmotreferansenrSftpTechnicalException("failed to list files, path: " + path, e);
        }
    }

    public String presentWorkingDirectory() {
        connectIfNotConnected();
        try {
            return channelSftp.pwd();
        } catch (SftpException e) {
            log.error(APPLICATION + " failed to get present working directory", e);
            throw new SkanmotreferansenrSftpTechnicalException("failed to get present working directory", e);
        }
    }

    public void changeDirectory(String path) {
        connectIfNotConnected();
        try {
            channelSftp.cd(path);
        } catch (SftpException e) {
            log.error(APPLICATION + " failed to change directory, path: " + path, e);
            throw new SkanmotreferansenrSftpTechnicalException("failed to change directory, path: " + path, e);
        }
    }


    public InputStream getFile(String filename) {
        connectIfNotConnected();
        try {
            return channelSftp.get(filename);
        } catch (SftpException e) {
            log.error(APPLICATION + " failed to download " + filename, e);
            throw new SkanmotreferansenrSftpTechnicalException("failed to download " + filename, e);
        }
    }

    public boolean isConnected() {
        return channelSftp.isConnected() && jschSession.isConnected();
    }

    public void connectIfNotConnected() {
        if (channelSftp == null || !channelSftp.isConnected()) {
            try {
                jschSession = jsch.getSession(username, host, Integer.parseInt(port));
                jsch.addIdentity(privateKey);
                jsch.setKnownHosts(hostKey);

                jschSession.connect();
                channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
                channelSftp.connect();
                setHomePath(channelSftp.getHome());
            } catch (JSchException | SftpException e) {
                log.error(APPLICATION + " failed to connect to " + host, e);
                throw new SkanmotreferansenrSftpTechnicalException("failed to connect to " + host, e);
            } catch (Exception e) {
                throw new SkanmotreferansenrSftpTechnicalException("Unknown error while connecting " + host, e);
            }
        }
    }

    public void disconnect() {
        if (channelSftp != null && channelSftp.isConnected()) {
            try {
                channelSftp.exit();
                jschSession.disconnect();
                log.info(APPLICATION + " disconnected from " + host);
            } catch (Exception e) {
                log.error(APPLICATION + " failed to disconnect from " + host, e);
                throw new SkanmotreferansenrSftpTechnicalException("failed to connect to " + host, e);
            }
        } else {
            log.warn(APPLICATION + " tried to disconnect while not connected");
        }
    }

    public String getHomePath() {
        connectIfNotConnected();
        return homePath;
    }


    public void deleteFile(String directory, String filename) {
        connectIfNotConnected();
        String filePath = directory + "/" + filename;
        try {
            channelSftp.rm(filePath);
        } catch (SftpException e) {
            log.error("{} klarte ikke slette {}", APPLICATION, filePath, e);
            throw new SkanmotreferansenrSftpTechnicalException("Klarte ikke slette " + filePath, e);
        }
    }

    public void uploadFile(InputStream file, String path, String filename) {
        connectIfNotConnected();
        createDirectoryIfNotExisting(path);
        try {
            channelSftp.put(file, path + "/" + filename);
        } catch (SftpException e) {
            log.error("{} klarte ikke laste opp fil {} til {}", APPLICATION, filename, path, e);
            throw new SkanmotreferansenrSftpTechnicalException("Klarte ikke laste opp fil", e);
        }
    }

    public void moveFile(String from, String to, String newFilename) {
        connectIfNotConnected();
        try {
            createDirectoryIfNotExisting(to);
            channelSftp.rename(from, to + "/" + newFilename);
        } catch (SftpException e) {
            log.error("{} klarte ikke flytte fil {} til {}", APPLICATION, from, to);
            throw new SkanmotreferansenrSftpTechnicalException("Klarte ikke flytte fil", e);
        }
    }

    private void createDirectoryIfNotExisting(String path) {
        connectIfNotConnected();
        try {
            channelSftp.lstat(path);
        } catch (SftpException mappeFinnesIkke) {
            // Path finnes ikke, så vi lager den. Kan bare lage en og en mappe
            String existingPath = "";
            for (String subPath : path.split("/")) {
                try {
                    channelSftp.lstat(existingPath + subPath);
                    existingPath += subPath + "/";
                } catch (SftpException delmappeFinnesIkke) {
                    try {
                        channelSftp.mkdir(existingPath + subPath);
                        existingPath += subPath + "/";
                    } catch (SftpException e) {
                        log.error("{} klarte ikke lage en ny mappe: {}", APPLICATION, path, e);
                        throw new SkanmotreferansenrSftpTechnicalException("Klarte ikke lage en ny mappe: " + path, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("{} klarte ikke lage en ny mappe: {}", APPLICATION, path, e);
            throw new SkanmotreferansenrSftpTechnicalException("Klarte ikke lage en ny mappe: " + path, e);
        }
    }

    // A bit hacky, but ChannelSftp does not handle windows paths very well.
    public void setHomePath(String homePath) {
        Pattern windowsFileSystemPattern = Pattern.compile("^[a-zA-Z]:/");
        Matcher windowsFileSystemMatcher = windowsFileSystemPattern.matcher(homePath);
        if (windowsFileSystemMatcher.find()) {
            this.homePath = homePath.substring(2);
        } else {
            this.homePath = homePath;
        }
    }

    @PreDestroy
    public void destroy() {
        disconnect();
    }
}
