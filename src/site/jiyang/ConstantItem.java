package site.jiyang;

/**
 * 常量项
 * 每个常量项都有 u1 的 tag, 表示其类型
 * <pre>
 * cp_info {
 *     u1 tag;
 *     u1 info[];
 * }
 * </pre>
 */
abstract class ConstantItem implements Parsable {
    final int tag;

    ConstantItem(int tag) {
        this.tag = tag;
    }

    abstract protected int contentSize();

    int size() {
        return 1/*u1的tag*/ + contentSize();
    }

    static ConstantItem getConstantItemTags(int tag) {
        switch (tag) {
            case 1:
                return new UTF8();
            case 3:
                return new INTEGER();
            case 4:
                return new FLOAT();
            case 5:
                return new LONG();
            case 6:
                return new DOUBLE();
            case 7:
                return new CLASS();
            case 8:
                return new STRING();
            case 9:
                return new FIELD_REF();
            case 10:
                return new METHOD_REF();
            case 11:
                return new Interface_Method_Ref();
            case 12:
                return new NAME_AND_TYPE();
            case 15:
                return new Method_Handle();
            case 16:
                return new Method_Type();
            case 18:
                return new Invoke_Dynamic();
            default:
                return null;
        }
    }

}

class UTF8 extends ConstantItem {
    private int length;
    public String value;

    UTF8() {
        super(1);
    }

    @Override
    protected int contentSize() {
        return length /* 字符串占用的字节数 */ + 2 /* u2 的字符串长度 attributeLength*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        length = Utils.read2Number(bytes, start + 1);
        value = new String(bytes, start + 3, length);
    }

    @Override
    public String toString() {
        return "Utf8{" +
                "attributeLength=" + length +
                ", value='" + value + '\'' +
                '}';
    }
}

class INTEGER extends ConstantItem {
    int value;

    protected INTEGER() {
        super(3);
    }

    @Override
    protected int contentSize() {
        return 4 /*u4 值*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        value = Utils.read4Number(bytes, start + 1);
    }

    @Override
    public String toString() {
        return "Integer{" +
                "value=" + value +
                '}';
    }
}

class FLOAT extends ConstantItem {
    float value;

    FLOAT() {
        super(4);
    }

    @Override
    protected int contentSize() {
        return 4 /* 同 INTEGER 的注释 */;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        value = Utils.read4Number(bytes, start + 1);
    }

    @Override
    public String toString() {
        return "Float{" +
                "value=" + value +
                '}';
    }
}

class LONG extends ConstantItem {
    long value;

    LONG() {
        super(5);
    }

    @Override
    protected int contentSize() {
        return 8 /* u8 值*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        value = Utils.read8Number(bytes, start + 1);
    }

    @Override
    public String toString() {
        return "Long{" +
                "value=" + value +
                '}';
    }
}

class DOUBLE extends ConstantItem {

    DOUBLE() {
        super(6);
    }

    double value;

    @Override
    protected int contentSize() {
        return 8 /*u8 值*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        value = Utils.read8Number(bytes, start + 1);
    }

    @Override
    public String toString() {
        return "Double{" +
                "value=" + value +
                '}';
    }
}

class CLASS extends ConstantItem {

    CLASS() {
        super(7);
    }

    int index;

    @Override
    protected int contentSize() {
        return 2 /*u2 常量池索引*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        index = Utils.read2Number(bytes, start + 1);
    }

    @Override
    public String toString() {
        return "Class{" +
                "index=" + index +
                '}';
    }
}

class STRING extends ConstantItem {
    STRING() {
        super(8);
    }

    int index; //u2

    @Override
    protected int contentSize() {
        return 2/*u2 常量池索引*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        index = Utils.read2Number(bytes, start + 1);
    }

    @Override
    public String toString() {
        return "String{" +
                "index=" + index +
                '}';
    }
}

class FIELD_REF extends ConstantItem {

    int classInfoIndex, nameAndTypeIndex;

    FIELD_REF() {
        super(9);
    }

    @Override
    protected int contentSize() {
        return 2/*u2 类信息常量池索引*/ + 2/*u2 名字和类型常量池索引*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        classInfoIndex = Utils.read2Number(bytes, start + 1);
        nameAndTypeIndex = Utils.read2Number(bytes, start + 3);
    }

    @Override
    public String toString() {
        return "FileRef{" +
                "classInfoIndex=" + classInfoIndex +
                ", nameAndTypeIndex=" + nameAndTypeIndex +
                '}';
    }
}

class METHOD_REF extends ConstantItem {
    int classInfoIndex, nameAndTypeIndex;

    METHOD_REF() {
        super(10);
    }

    @Override
    protected int contentSize() {
        return 4 /*同 FIELD_REF*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        classInfoIndex = Utils.read2Number(bytes, start + 1);
        nameAndTypeIndex = Utils.read2Number(bytes, start + 3);
    }

    @Override
    public String toString() {
        return "MethodRef{" +
                "classInfoIndex=" + classInfoIndex +
                ", nameAndTypeIndex=" + nameAndTypeIndex +
                '}';
    }
}

class Interface_Method_Ref extends ConstantItem {
    int classInfoIndex, nameAndTypeIndex;

    Interface_Method_Ref() {
        super(11);
    }

    @Override
    protected int contentSize() {
        return 4/*同FIELD_REF*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        classInfoIndex = Utils.read2Number(bytes, start + 1);
        nameAndTypeIndex = Utils.read2Number(bytes, start + 3);
    }

    @Override
    public String toString() {
        return "InterfaceMethodRef{" +
                "classInfoIndex=" + classInfoIndex +
                ", nameAndTypeIndex=" + nameAndTypeIndex +
                '}';
    }
}

class NAME_AND_TYPE extends ConstantItem {
    int nameIndex, descriptorIndex;

    NAME_AND_TYPE() {
        super(12);
    }

    @Override
    protected int contentSize() {
        return 2/*u2 名称在常量池的索引*/ + 2/*u2 描述符在常量池的索引*/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        nameIndex = Utils.read2Number(bytes, start + 1);
        descriptorIndex = Utils.read2Number(bytes, start + 3);
    }

    @Override
    public String toString() {
        return "NameAndType{" +
                "nameIndex=" + nameIndex +
                ", descriptorIndex=" + descriptorIndex +
                '}';
    }
}

class Method_Handle extends ConstantItem {

    int referenceKind; //unsigned byte
    int referenceIndex;

    Method_Handle() {
        super(15);
    }

    @Override
    protected int contentSize() {
        return 1/**/ + 2/**/;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        referenceKind = Utils.readUnsignedByte(bytes, start + 1);
        referenceIndex = Utils.read2Number(bytes, start + 2);
    }

    @Override
    public String toString() {
        return "MethodHandle{" +
                "referenceKind=" + referenceKind +
                ", referenceIndex=" + referenceIndex +
                '}';
    }
}

class Method_Type extends ConstantItem {
    int descriptorIndex;

    Method_Type() {
        super(16);
    }

    @Override
    protected int contentSize() {
        return 2;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        descriptorIndex = Utils.read2Number(bytes, start + 1);
    }

    @Override
    public String toString() {
        return "MethodType{" +
                "descriptorIndex=" + descriptorIndex +
                '}';
    }
}

class Invoke_Dynamic extends ConstantItem {
    int bootstrapAttrIndex, nameAndTypeIndex;

    Invoke_Dynamic() {
        super(18);
    }

    @Override
    protected int contentSize() {
        return 4;
    }

    @Override
    public void parse(byte[] bytes, int start) {
        bootstrapAttrIndex = Utils.read2Number(bytes, start + 1);
        nameAndTypeIndex = Utils.read2Number(bytes, start + 3);
    }

    @Override
    public String toString() {
        return "InvokeDynamic{" +
                "bootstrapAttrIndex=" + bootstrapAttrIndex +
                ", nameAndTypeIndex=" + nameAndTypeIndex +
                '}';
    }
}
