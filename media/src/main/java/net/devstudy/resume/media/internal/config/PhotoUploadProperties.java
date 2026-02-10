package net.devstudy.resume.media.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import net.devstudy.resume.shared.constants.Constants;

@ConfigurationProperties(prefix = "upload.photos")
public class PhotoUploadProperties {

    private String dir = "uploads/photos";
    private int smallWidth = Constants.UIImageType.AVATARS.getSmallWidth();
    private int smallHeight = Constants.UIImageType.AVATARS.getSmallHeight();
    private int largeWidth = Constants.UIImageType.AVATARS.getLargeWidth();
    private int largeHeight = Constants.UIImageType.AVATARS.getLargeHeight();

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public int getSmallWidth() {
        return smallWidth;
    }

    public void setSmallWidth(int smallWidth) {
        this.smallWidth = smallWidth;
    }

    public int getSmallHeight() {
        return smallHeight;
    }

    public void setSmallHeight(int smallHeight) {
        this.smallHeight = smallHeight;
    }

    public int getLargeWidth() {
        return largeWidth;
    }

    public void setLargeWidth(int largeWidth) {
        this.largeWidth = largeWidth;
    }

    public int getLargeHeight() {
        return largeHeight;
    }

    public void setLargeHeight(int largeHeight) {
        this.largeHeight = largeHeight;
    }
}
