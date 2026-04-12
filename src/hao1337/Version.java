package hao1337;

public class Version {
    /** Github API url to this mod repo */
    public static final String gitapi = "https://api.github.com/repos/Hao-1337/mindustry-better-vanilla/releases";
    /** Github repo name */
    public static final String repoName = "hao1337/mindustry-better-vanilla";
    /** Mod name that will get use as id in game */
    public static final String name = "hao1337-mod";
    /** Zip file name (using for auto update if user already unzip the mod file) */
    public static final String unzipName = "hao-1337mindustry-better-vanilla";
    /** Mod maajor version */
    public static final int majorVersion = 1;
    /** Mod minor version */
    public static final int minorVersion = 9;
    /** Mod patch version */
    public static final int patchVersion = 2;
    /** Mod version tag */
    public static final Tag tag = Tag.RELEASE;
    /** Target vendor using in this mod version */
    public static final Vendor vendor = Vendor.ANDROID;

    public static class InvalidVersionStringException extends Exception {
        public InvalidVersionStringException(String version) {
            super("Invalid version string: " + version);
        }
    }

    public static enum Vendor {
        DESKTOP,
        ANDROID;

        public static Vendor parseVendor(String version) {
            if (version.contains("steam") || version.contains("desktop"))
                return DESKTOP;
            return ANDROID;
        }
    }

    public static enum Tag {
        RELEASE(2),
        BETA(1),
        ALPHA(0);

        private final int priority;

        private Tag(int priority) {
            this.priority = priority;
        }

        public int priority() {
            return priority;
        }

        public static Tag parseTag(String version) {
            if (version.contains("-alpha"))
                return ALPHA;
            if (version.contains("-beta"))
                return BETA;
            return RELEASE;
        }
    }

    private static class Parsed {
        int major, minor, patch;
        Tag tag;
        Vendor vendor;
    }

    private static Parsed parse(String version) throws InvalidVersionStringException {
        try {
            Parsed p = new Parsed();

            String v = version.startsWith("v") ? version.substring(1) : version;

            p.tag = Tag.parseTag(v);
            p.vendor = Vendor.parseVendor(v);

            String numbers = v.split("-")[0];
            String[] parts = numbers.split("\\.");

            if (parts.length < 3)
                throw new InvalidVersionStringException(version);

            p.major = Integer.parseInt(parts[0]);
            p.minor = Integer.parseInt(parts[1]);
            p.patch = Integer.parseInt(parts[2]);

            return p;
        } catch (Exception e) {
            throw new InvalidVersionStringException(version);
        }
    }

    public static int compare(String v1, String v2) throws InvalidVersionStringException {
        Parsed a = parse(v1);
        Parsed b = parse(v2);

        if (a.major != b.major)
            return Integer.compare(a.major, b.major);

        if (a.minor != b.minor)
            return Integer.compare(a.minor, b.minor);

        if (a.patch != b.patch)
            return Integer.compare(a.patch, b.patch);

        int tagCompare = Integer.compare(a.tag.priority(), b.tag.priority());
        if (tagCompare != 0)
            return tagCompare;

        return a.vendor.compareTo(b.vendor);
    }

    private static int compare(String version) throws InvalidVersionStringException {
        Parsed remote = parse(version);

        if (remote.major != majorVersion)
            return Integer.compare(remote.major, majorVersion);

        if (remote.minor != minorVersion)
            return Integer.compare(remote.minor, minorVersion);

        if (remote.patch != patchVersion)
            return Integer.compare(remote.patch, patchVersion);

        int tagCompare = Integer.compare(remote.tag.priority(), tag.priority());
        if (tagCompare != 0)
            return tagCompare;

        return remote.vendor.compareTo(vendor);
    }

    public static boolean isSmaller(String version) throws InvalidVersionStringException {
        return compare(version) < 0;
    }

    public static boolean isLarger(String version) throws InvalidVersionStringException {
        return compare(version) > 0;
    }

    public static boolean isLargerOrEqual(String version) throws InvalidVersionStringException {
        return compare(version) >= 0;
    }

    public static boolean isSameVendor(String version) {
        try {
            Parsed parsed = parse(version);
            return parsed.vendor == vendor;
        } catch (InvalidVersionStringException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean equal(String version) {
        try {
            return compare(version) == 0;
        } catch (InvalidVersionStringException ex) {
            return false;
        }
    }

    public static boolean equalIgnoreVendor(String version) {
        try {
            Parsed p = parse(version);

            return p.major == majorVersion
                    && p.minor == minorVersion
                    && p.patch == patchVersion
                    && p.tag == tag;
        } catch (InvalidVersionStringException e) {
            return false;
        }
    }

    public static String getNoVendorVersionString() {
        StringBuilder v = new StringBuilder();

        v.append("v")
                .append(majorVersion).append(".")
                .append(minorVersion).append(".")
                .append(patchVersion);

        if (tag == Tag.ALPHA)
            v.append("-alpha");
        else if (tag == Tag.BETA)
            v.append("-beta");
        return v.toString();
    }

    public static String getVersionString() {
        StringBuilder v = new StringBuilder();

        v.append("v")
                .append(majorVersion).append(".")
                .append(minorVersion).append(".")
                .append(patchVersion);

        if (tag == Tag.ALPHA)
            v.append("-alpha");
        else if (tag == Tag.BETA)
            v.append("-beta");

        if (vendor == Vendor.DESKTOP)
            v.append("-desktop");

        return v.toString();
    }
}
