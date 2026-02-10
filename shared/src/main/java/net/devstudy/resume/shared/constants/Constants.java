package net.devstudy.resume.shared.constants;

public final class Constants {
    private Constants() {}

    public static final String[] EMPTY_ARRAY = {};

    public enum UIImageType {
        AVATARS(110, 110, 400, 400),
        CERTIFICATES(142, 100, 900, 400);

        private final int smallWidth;
        private final int smallHeight;
        private final int largeWidth;
        private final int largeHeight;

        UIImageType(int smallWidth, int smallHeight, int largeWidth, int largeHeight) {
            this.smallWidth = smallWidth;
            this.smallHeight = smallHeight;
            this.largeWidth = largeWidth;
            this.largeHeight = largeHeight;
        }

        public String getFolderName() {
            return name().toLowerCase();
        }

        public int getSmallWidth() {
            return smallWidth;
        }

        public int getSmallHeight() {
            return smallHeight;
        }

        public int getLargeWidth() {
            return largeWidth;
        }

        public int getLargeHeight() {
            return largeHeight;
        }
    }

    public static final class UI {
        private UI() {}
        public static final int MAX_PROFILES_PER_PAGE = 10;
        public static final String USER = "USER";
    }
}
