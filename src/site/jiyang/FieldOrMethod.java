package site.jiyang;


import java.util.ArrayList;

/**
 * <pre>
 * field_info {
 *     u2             access_flags;
 *     u2             name_index;
 *     u2             descriptor_index;
 *     u2             attributes_count;
 *     attribute_info attributes[attributes_count];
 * }
 * </pre>
 */
class FieldOrMethod extends Section {

    private int infoSize = 0;

    int count; //u2

    static class Entity {

        int accessFlag;  //u2
        int nameIndex;   //u2
        int descriptorIndex; //u2
        int attributesCount; //u2

        private ArrayList<AttributeInfo> mAttributeInfos = new ArrayList<>();

        public int size() {
            int infoSize = 0;
            for (AttributeInfo info : mAttributeInfos) {
                infoSize += info.size();
            }
            return 8 + infoSize;
        }

        public void parse(byte[] bytes, int offset) {
            accessFlag = Utils.read2Number(bytes, offset);
            offset += 2;
            nameIndex = Utils.read2Number(bytes, offset);
            offset += 2;
            descriptorIndex = Utils.read2Number(bytes, offset);
            offset += 2;
            attributesCount = Utils.read2Number(bytes, offset);
            offset += 2;
            for (int i = 0; i < attributesCount; i++) {
                AttributeInfo attributeInfo = new AttributeInfo();
                attributeInfo.parse(bytes, offset);
                mAttributeInfos.add(attributeInfo);
                offset += attributeInfo.size();
            }
        }

        static final int PUBLIC = 0x0001;
        static final int PRIVATE = 0x0002;
        static final int PROTECTED = 0x0004;
        static final int STATIC = 0x0008;
        static final int FINAL = 0x0010;
        // field special
        static final int VOLATILE = 0x0040;
        static final int TRANSIENT = 0x0080;
        static final int SYNTHETIC = 0x1000;
        static final int ENUM = 0x4000;
        // method special
        static final int SYNCHRONIZED = 0x0020;
        static final int BRIDGE = 0x0040;
        static final int VARARGS = 0x0080;
        static final int NATIVE = 0x0100;
        static final int ABSTRACT = 0x0400;
        static final int STRICTFP = 0x0500;

        private static String accessFlagReadable(int access) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            if ((access & PUBLIC) == PUBLIC) sb.append("Public,");
            if ((access & PRIVATE) == PRIVATE) sb.append("Private,");
            if ((access & PROTECTED) == PROTECTED) sb.append("Protected,");
            if ((access & STATIC) == STATIC) sb.append("Static,");
            if ((access & FINAL) == FINAL) sb.append("Final,");
            if ((access & VOLATILE) == VOLATILE) sb.append("Volatile,");
            if ((access & TRANSIENT) == TRANSIENT) sb.append("Transient,");
            if ((access & SYNTHETIC) == SYNTHETIC) sb.append("Synthetic,");
            if ((access & ENUM) == ENUM) sb.append("Enum,");
            if ((access & SYNCHRONIZED) == SYNCHRONIZED) sb.append(", Synchronized");
            if ((access & BRIDGE) == BRIDGE) sb.append(",Bridge");
            if ((access & VARARGS) == VARARGS) sb.append(",Varargs");
            if ((access & NATIVE) == NATIVE) sb.append(",Native");
            if ((access & ABSTRACT) == ABSTRACT) sb.append(",Abstract");
            if ((access & STRICTFP) == STRICTFP) sb.append(",Strictfp");

            sb.append("]");
            return sb.toString();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (AttributeInfo attributeInfo : mAttributeInfos) {
                sb.append("    ").append(attributeInfo.toString()).append("\n");
            }
            return "Entity{" +
                    "accessFlag=" + accessFlagReadable(accessFlag) +
                    ", nameIndex=" + nameIndex +
                    ", name=" + ((UTF8) BytecodeParser.constantItemHashMap.get(nameIndex)).value +
                    ", descriptorIndex=" + descriptorIndex +
                    ", attributesCount=" + attributesCount +
                    ", mAttributeInfos=\n" + sb.toString() +
                    '}';
        }
    }

    private final String name;
    private final ArrayList<Entity> mEntities = new ArrayList<>();

    public FieldOrMethod(String name, int start, byte[] bytes) {
        super(start, bytes);
        this.name = name;
    }

    @Override
    int size() {
        return infoSize + 2;
    }

    @Override
    public void parse() {
        int offset = start;
        count = Utils.read2Number(bytes, offset);
        offset += 2;
        for (int i = 0; i < count; i++) {
            Entity entity = new Entity();
            entity.parse(bytes, offset);
            mEntities.add(entity);
            infoSize += entity.size();
            offset += entity.size();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entity entity : mEntities) {
            sb.append("  ").append(name).append(" ").append(entity.toString()).append("\n");
        }
        return "FieldOrMethod{" +
                "count=" + count +
                ", name='" + name + '\'' +
                ", mEntities=\n" + sb.toString() +
                '}';
    }
}

