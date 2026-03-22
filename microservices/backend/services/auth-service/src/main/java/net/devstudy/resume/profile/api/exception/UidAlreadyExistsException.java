package net.devstudy.resume.profile.api.exception;

public class UidAlreadyExistsException extends IllegalArgumentException {

    private final String uid;

    public UidAlreadyExistsException(String uid) {
        super("Uid already exists: " + uid);
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }
}
