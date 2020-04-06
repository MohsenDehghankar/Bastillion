package io.bastillion.manage.model;


public class FileTransferAuditWrapper {
    User user;
    HostSystem hostSystem;
    String sourcePath;
    String destPath;
    Long sessionID;
    // upload or download
    String type;

    public FileTransferAuditWrapper(User user, HostSystem hostSystem, String sourcePath,
                                    String destPath, Long sessionID, String type) {
        this.user = user;
        this.hostSystem = hostSystem;
        this.sourcePath = sourcePath;
        this.destPath = destPath;
        this.sessionID = sessionID;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public User getUser() {
        return user;
    }

    public HostSystem getHostSystem() {
        return hostSystem;
    }

    public String getDestPath() {
        return destPath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public Long getSessionID() {
        return sessionID;
    }
}
