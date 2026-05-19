package com.ezflytime.update;

public class UpdateCheckResult {

    public enum Status {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        FAILED
    }

    private final Status status;
    private final String currentVersion;
    private final String latestVersion;
    private final String errorMessage;
    private final String downloadUrl;

    private UpdateCheckResult(Status status, String currentVersion, String latestVersion, String errorMessage, String downloadUrl) {
        this.status = status;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.errorMessage = errorMessage;
        this.downloadUrl = downloadUrl;
    }

    public static UpdateCheckResult upToDate(String currentVersion, String latestVersion, String downloadUrl) {
        return new UpdateCheckResult(Status.UP_TO_DATE, currentVersion, latestVersion, null, downloadUrl);
    }

    public static UpdateCheckResult updateAvailable(String currentVersion, String latestVersion, String downloadUrl) {
        return new UpdateCheckResult(Status.UPDATE_AVAILABLE, currentVersion, latestVersion, null, downloadUrl);
    }

    public static UpdateCheckResult failed(String currentVersion, String errorMessage, String downloadUrl) {
        return new UpdateCheckResult(Status.FAILED, currentVersion, null, errorMessage, downloadUrl);
    }

    public Status getStatus() {
        return status;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
