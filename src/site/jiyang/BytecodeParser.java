package site.jiyang;


import java.util.Arrays;
import java.util.HashMap;

/**
 * <pre>
 * ClassFile {
 *     u4             magic;
 *     u2             minor_version;
 *     u2             major_version;
 *     u2             constant_pool_count;
 *     cp_info        constant_pool[constant_pool_count-1];
 *     u2             access_flags;
 *     u2             this_class;
 *     u2             super_class;
 *     u2             interfaces_count;
 *     u2             interfaces[interfaces_count];
 *     u2             fields_count;
 *     field_info     fields[fields_count];
 *     u2             methods_count;
 *     method_info    methods[methods_count];
 *     u2             attributes_count;
 *     attribute_info attributes[attributes_count];
 * }
 * </pre>
 */
public final class BytecodeParser {
    public static HashMap<Integer, ConstantItem> constantItemHashMap;

    public void parse(final byte[] bytes) {
        printSectionDivider("Magic Number");
        Section magicNumber = new MagicNumber(0, bytes);
        magicNumber.parse();
        System.out.println(magicNumber);

        printSectionDivider("Version");
        Section version = new Version(magicNumber.end(), bytes);
        version.parse();
        System.out.println(version);

        printSectionDivider("Constant Pool");
        ConstantPool constantPool = new ConstantPool(version.end(), bytes);
        constantPool.parse();
        System.out.println(constantPool);
        constantItemHashMap = constantPool.getConstantItems();

        printSectionDivider("Access Flags");
        Section accessFlags = new AccessFlags(constantPool.end(), bytes);
        accessFlags.parse();
        System.out.println(accessFlags);

        printSectionDivider("This class");
        Section thisClass = new ClassIndex(accessFlags.end(), bytes);
        thisClass.parse();
        System.out.println(thisClass);

        printSectionDivider("Super class");
        Section superClass = new ClassIndex(thisClass.end(), bytes);
        superClass.parse();
        System.out.println(superClass);

        printSectionDivider("Interfaces");
        Interfaces interfaces = new Interfaces(superClass.end(), bytes);
        interfaces.parse();
        System.out.println(interfaces);

        printSectionDivider("Fields");
        FieldOrMethod fields = new FieldOrMethod("Fields", interfaces.end(), bytes);
        fields.parse();
        System.out.println(fields);

        printSectionDivider("Methods");
        FieldOrMethod methods = new FieldOrMethod("Methods", fields.end(), bytes);
        methods.parse();
        System.out.println(methods);

        printSectionDivider("This class attribute_info");
        int offset = methods.end();
        int attributeCount = Utils.read2Number(bytes, offset);
        offset += 2;
        AttributeInfo[] attributeInfos = new AttributeInfo[attributeCount];
        for (int i = 0; i < attributeCount; i++) {
            attributeInfos[i] = new AttributeInfo();
            attributeInfos[i].parse(bytes, offset);
            offset += attributeInfos[i].size();
        }
        System.out.println(Arrays.toString(attributeInfos));
    }

    private static void printSectionDivider(String name) {
        System.out.println("== " + name + " ======================================");
    }

}

interface Parsable {
    public void parse(byte[] bytes, int offset);
}

abstract class Section {
    final int start;
    final byte[] bytes;

    public Section(int start, byte[] bytes) {
        this.start = start;
        this.bytes = bytes;
    }

    public final int start() {
        return start;
    }

    public final int end() {
        return start + size();
    }

    abstract int size();

    abstract public void parse();
}

class Utils {
    /**
     * 获取占4字节的数
     */
    static int read4Number(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16) | ((bytes[offset + 2] & 0xFF) << 8) | ((bytes[offset + 3] & 0xFF));
    }

    /**
     * 获取占2字节的数, 为了避免 java 中只有 signed short 越界出现显示了负数, 所以返回 int
     */
    static int read2Number(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    static int readUnsignedByte(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF);
    }

    /**
     * 获得占8字节的数
     */
    static long read8Number(byte[] bytes, int offset) {
        return (
                ((long) bytes[offset] & 0xFF) << 56) |
                (((long) bytes[offset + 1] & 0xFF) << 48) |
                (((long) bytes[offset + 2] & 0xFF) << 40) |
                (((long) bytes[offset + 3] & 0xFF) << 32) |
                (((long) bytes[offset + 3] & 0xFF) << 24) |
                (((long) bytes[offset + 3] & 0xFF) << 16) |
                (((long) bytes[offset + 3] & 0xFF) << 8) |
                (((long) bytes[offset + 3] & 0xFF));
    }
}
