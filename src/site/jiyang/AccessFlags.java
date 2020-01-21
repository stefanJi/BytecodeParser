package site.jiyang;


import java.util.ArrayList;

class AccessFlags extends Section {

    private static final int PUBLIC = 0x0001;    // 0000 0000 0000 0001
    private static final int FINAL = 0x0010;     // 0000 0000 0001 0000
    private static final int SUPER = 0x0020;     // 0000 0000 0010 0000
    private static final int INTERFACE = 0x0200; // 0000 0010 0000 0000
    private static final int ABSTRACT = 0x0400;  // 0000 0100 0000 0000
    private static final int SYNTHETIC = 0x1000; // 0001 0000 0000 0000
    private static final int ANNOTATION = 0x2000;// 0010 0000 0000 0000
    private static final int ENUM = 0x4000;      // 0100 0000 0000 0000
    private static final int PRIVATE = 0x0002;

    private int accessFlags;

    public AccessFlags(int start, byte[] bytes) {
        super(start, bytes);
    }

    @Override
    int size() {
        return 2;
    }

    @Override
    public void parse() {
        accessFlags = Utils.read2Number(bytes, start);
    }

    @Override
    public String toString() {
        return "AccessFlags{" +
                "accessFlags=" + accessFlags +
                '}';
    }

    static String printAccess(int access) {
        ArrayList<String> accessFlags = new ArrayList<>();
        if ((access & PUBLIC) == PUBLIC) {
            accessFlags.add("public");
        }
        if ((access & FINAL) == FINAL) {
            accessFlags.add("final");
        }
        if ((access & SUPER) == SUPER) {
            accessFlags.add("super");
        }
        if ((access & INTERFACE) == INTERFACE) {
            accessFlags.add("interface");
        }
        if ((access & ABSTRACT) == ABSTRACT) {
            accessFlags.add("abstract");
        }
        if ((access & SYNTHETIC) == SYNTHETIC) {
            accessFlags.add("synthetic");
        }
        if ((access & ANNOTATION) == ANNOTATION) {
            accessFlags.add("annotation");
        }
        if ((access & ENUM) == ENUM) {
            accessFlags.add("enum");
        }
        if ((access & PRIVATE) == PRIVATE) {
            accessFlags.add("private");
        }
        StringBuilder sb = new StringBuilder();
        accessFlags.forEach(s -> sb.append(s).append(","));
        return sb.toString();
    }
}
