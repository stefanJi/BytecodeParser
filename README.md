# 使用 Java 解析字节码文件结构

按照JVM 字节码的存储格式 https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html 规范，实现了一段程序解析字节码结构。

运行:

```
./run.sh
```

> 因为字节码指令太多了，所以还没有实现解析每一个字节码指令(`Code`属性里面的内容)。[Instruction.java](.src/site/jiyang/Instruction.java) 实现了一个字节码指令(ldc)的解析，其他指令类似。

## 结果输出

```
========== Start Parse out/site/jiyang/Main.class =========
== Magic Number ======================================
MagicNumber{b1=CA, b2=FE, b3=BA, b4=BE}
== Version ======================================
Version{minorVersion=0, majorVersion=52}
== Constant Pool ======================================
ConstantPool{poolCount=214, constantsSize=2376, mConstantItems=
  #1 MethodRef{classInfoIndex=65, nameAndTypeIndex=130}
  #2 String{index=131}
  ...
  #213 Utf8{attributeLength=20, value='()Ljava/lang/String;'}
}
== Access Flags ======================================
AccessFlags: public,final,super,
bytecode.AccessFlags@27bc2616
== This class ======================================
ClassIndex{classInfoIndex=49 -> 171 -> bytecode/BytecodeParser}
== Super class ======================================
ClassIndex{classInfoIndex=65 -> 185 -> java/lang/Object}
== Interfaces ======================================
Interface count: 0
Interface Indexes: []
== Fields ======================================
FieldOrMethod{count=2, name='Fields', mEntities=[Entity{accessFlag=[Public,Static,], nameIndex=66, name=constantItemHashMap, descriptorIndex=67, attributesCount=1, mAttributeInfos=[AttributeInfo{nameIndex=68, attributeLength=2, mInfo=Signature{signatureIndex=69 -> Ljava/util/HashMap<Ljava/lang/Integer;Lbytecode/ConstantItem;>;}}]}, Entity{accessFlag=[Private,Static,Final,], nameIndex=70, name=path, descriptorIndex=71, attributesCount=1, mAttributeInfos=[AttributeInfo{nameIndex=72, attributeLength=2, mInfo=ConstantValue{constantValueIndex=4}}]}]}
== Methods ======================================
FieldOrMethod{count=4, name='Methods', mEntities=[Entity{accessFlag=[Public,], nameIndex=73,...
== This class attribute_info ======================================
[AttributeInfo{nameIndex=128, attributeLength=2, mInfo=SourceFile{sourceFileIndex=129 -> BytecodeParser.java}}]
```

## 实现

### 整体流程

```
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```
按照字节码的储存顺序依次解析每一块内容。所有解析过程都共用一个 `byte[]` 数组，每个具体的解析过程，通过控制 `offset` 实现解析不同部分的数据。

```java
public final class BytecodeParser {
    // 用来在常量池解析完之后，存在全局方便后面使用
    public static HashMap<Integer, ConstantItem> constantItemHashMap;

    private void parse(final byte[] bytes) {
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
}
```

```java
public static void main(String[] args) {
    if (args.length < 1) {
        throw new IllegalArgumentException("Must pass class file path.");
    }
    String path = args[0];
    System.out.println("========== Start Parse=========");
    try {
        FileInputStream fis = new FileInputStream(new File(path));
        int count = fis.available();
        byte[] buff = new byte[count];
        int read = fis.read(buff);
        if (read != count) {
            return;
        }
        new BytecodeParser().parse(buff);
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private static void printSectionDivider(String name) {
    System.out.println("== " + name + " ======================================");
}
```
### 提取了一些抽象

方便后面实现时生成统一的方法

```java
interface Parsable {
    public void parse(byte[] bytes, int offset);
}
```

```java
abstract class Section {
    final int start;    //记录每一块结构的开始位置
    final byte[] bytes; // 所有人共用的数据

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

    abstract int size();    //每一块结构需要返回自己占用了多少字节

    abstract public void parse();
}
```

### 解析魔数

```java
class MagicNumber extends Section {

    public MagicNumber(int start, byte[] bytes) {
        super(start, bytes);
    }

    @Override
    int size() {
        return 4;
    }

    private int b1, b2, b3, b4;//u1

    @Override
    public void parse() {
        b1 = Utils.readUnsignedByte(bytes, start);
        b2 = Utils.readUnsignedByte(bytes, start + 1);
        b3 = Utils.readUnsignedByte(bytes, start + 2);
        b4 = Utils.readUnsignedByte(bytes, start + 3);
    }

    @Override
    public String toString() {
        return "MagicNumber{" +
                "b1=" + Integer.toHexString(b1).toUpperCase() +
                ", b2=" + Integer.toHexString(b2).toUpperCase() +
                ", b3=" + Integer.toHexString(b3).toUpperCase() +
                ", b4=" + Integer.toHexString(b4).toUpperCase() +
                '}';
    }
}
```

### 解析版本号

```java
class Version extends Section {

    public Version(int start, byte[] bytes) {
        super(start, bytes);
    }

    @Override
    int size() {
        return 4;
    }

    private int minorVersion, majorVersion; //u2

    @Override
    public void parse() {
        minorVersion = Utils.read2Number(bytes, start);
        majorVersion = Utils.read2Number(bytes, start + 2);
    }

    @Override
    public String toString() {
        return "Version{" +
                "minorVersion=" + minorVersion +
                ", majorVersion=" + majorVersion +
                '}';
    }
}
```

### 解析常量池

常量池这里有一点要注意：代表常量池有多少常量的 poolCount，是包括了 poolCount 它本身的。也就是真正的常量池的常量其实是 `poolCount -1` 个。

每个常量项都有一个 u1 的 tag 表示其类型。解析时需要先解析 tag, 然后根据 tag 做对应类型的解析。

```java
class ConstantPool extends Section {

    int poolCount; //u2
    private int constantsSize;

    private HashMap<Integer, ConstantItem> mConstantItems = new HashMap<>();

    public HashMap<Integer, ConstantItem> getConstantItems() {
        return mConstantItems;
    }

    public ConstantPool(int start, byte[] bytes) {
        super(start, bytes);
    }

    @Override
    int size() {
        return 2/*u2 的常量池计数占用*/ + constantsSize;
    }

    @Override
    public void parse() {
        poolCount = Utils.read2Number(bytes, start);
        // 遍历常量表的每一项常量
        int offset = 2;
        for (int i = 1; i <= poolCount - 1; i++) {
            int tag = Utils.readUnsignedByte(bytes, start + offset);
            // 根据 tag 找到匹配的常量
            ConstantItem item = ConstantItem.getConstantItemTags(tag);
            if (item == null) {
                System.err.println("Not found ConstantItem for " + tag);
                return;
            }
            item.parse(bytes, start + offset);
            offset += item.size();
            constantsSize += item.size();
            mConstantItems.put(i, item);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= poolCount - 1; i++) {
            sb.append("  #").append(i).append(" ").append(mConstantItems.get(i)).append("\n");
        }
        return "ConstantPool{" +
                "poolCount=" + poolCount +
                ", constantsSize=" + constantsSize +
                ", mConstantItems=\n" + sb.toString() +
                '}';
    }
}
```

#### 每个常量项

```java
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

    @Nullable
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
```

#### CONSTANT_Utf8

所有的字面量都储存在 UTF8 常量中。`value` 存储着以 UTF-8 编码的字符串的原始字节数据。

```java
//region Constants

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
```
#### CONSTANT_Integer

```java
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
```

### CONSTANT_Float

```java
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
```

#### CONSTANT_Long

```java
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
```
#### CONSTANT_Double

```java
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
```

#### CONSTANT_Class

存储指向类的全限定名在常量池中的索引 `index`

```
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
```
#### CONSTANT_String

存储指向 Constant_UTF8 的索引 `index`

```java
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
```
#### CONSTANT_Fieldref

- classInfoIndex 声明字段的类的信息，指向 CONSTANT_Class
- nameAndTypeIndex 字段的信息，指向 CONSTANT_NameAndType 类型的索引

```
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
```

#### CONSTANT_Methodref

- classInfoIndex 声明方法的类的信息，指向 CONSTANT_Class
- nameAndTypeIndex 方法的信息，指向 CONSTANT_NameAndType 类型的索引

```
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
```

#### CONSTANT_InterfaceMethodref

- classInfoIndex 声明接口的类的信息，指向 CONSTANT_Class
- nameAndTypeIndex 接口的信息，指向 CONSTANT_NameAndType 类型的索引

```
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
```

#### CONSTANT_NameAndType

- nameIndex 指向方法或字段的名称在常量池中的索引
- descriptorIndex 指向方法或字段的描述符在常量池中的索引

```
class NAME_AND_TYPE extends ConstantItem {
    int nameIndex, descriptorIndex; //u2

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
```

#### CONSTANT_MethodHandle

详细信息参考 https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.8 

- referenceKind 方法句柄的类型，代表字节码被执行时的行为. ? 不太懂这个... 还没有遇到出现的情况
- referenceIndex 
```java
class Method_Handle extends ConstantItem {

    int referenceKind; //u1
    int referenceIndex; //u2

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
```

#### CONSTANT_MethodType

- descriptorIndex 指向常量池中 UTF8 类型的索引，表示方法的描述符

```java
class Method_Type extends ConstantItem {
    int descriptorIndex; //u2

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
```

#### CONSTANT_InvokeDynamic

https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.10

与 Java 的动态方法调用有关。

```
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

//endregion
```

### 解析访问标志

一个字段或方法或类的访问标志都是使用一个 `u2` 类型的数字表示，通过位运算 `|` 赋予，通过 `&`运算判断是否拥有某种访问标志。

```java
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

    private int accessFlags; //u2

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
        System.out.println("AccessFlags: " + printAccess(accessFlags));
    }

    public static String printAccess(int access) {
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
```

### 解析类信息

类自身和其父类都存储在 class_info 结构中，`classInfoIndex` 指向常量池中 `Constant_Class` 类型的常量

```java
/**
 * 类索引
 * 用于解析自身类和父类在常量池的索引
 */
class ClassIndex extends Section {

    private int classInfoIndex;

    public ClassIndex(int start, byte[] bytes) {
        super(start, bytes);
    }

    @Override
    int size() {
        return 2;
    }

    @Override
    public void parse() {
        classInfoIndex = Utils.read2Number(bytes, start);
    }

    @Override
    public String toString() {
        CLASS clasz = (CLASS) BytecodeParser.constantItemHashMap.get(classInfoIndex);
        UTF8 utf8 = (UTF8) BytecodeParser.constantItemHashMap.get(clasz.index);
        return "ClassIndex{" +
                "classInfoIndex=" + classInfoIndex +
                " -> " + clasz.index + " -> " + utf8.value +
                '}';
    }
}
```

### 解析接口

类实现的所有接口在常量池的 Constant_class 类型索引

```java
class Interfaces extends Section {

    int interfaceCount; //u2
    int[] indexs; // u2[interfaceCount]

    public Interfaces(int start, byte[] bytes) {
        super(start, bytes);
    }

    @Override
    int size() {
        return interfaceCount * 2 + 2;
    }

    @Override
    public void parse() {
        interfaceCount = Utils.read2Number(bytes, start);
        int offset = 2;
        indexs = new int[interfaceCount];
        for (short i = 0; i < interfaceCount; i++) {
            int index = Utils.read2Number(bytes, start + offset);
            indexs[i] = index;
        }
    }

    @Override
    public String toString() {
        return "Interfaces{" +
                "interfaceCount=" + interfaceCount +
                ", indexs=" + Arrays.toString(indexs) +
                '}';
    }
}
```

### 解析方法和字段

```java
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
            return "Entity{" +
                    "accessFlag=" + accessFlagReadable(accessFlag) +
                    ", nameIndex=" + nameIndex +
                    ", name=" + ((UTF8) BytecodeParser.constantItemHashMap.get(nameIndex)).value +
                    ", descriptorIndex=" + descriptorIndex +
                    ", attributesCount=" + attributesCount +
                    ", mAttributeInfos=" + mAttributeInfos +
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
        return "FieldOrMethod{" +
                "count=" + count +
                ", name='" + name + '\'' +
                ", mEntities=" + mEntities +
                '}';
    }
}
```

### 解析属性表

字段、方法、类都能拥有自己的属性表。只是某些属性只会出现在字段上(比如 *volatile*)或方法上(比如 *synchronized*)。

```
u2 attribute_name_index;
u4 attribute_length;
```
`attribute_name_index` 和 `attribute_length` 是每个属性都有的两个字段，所以提出来由父类实现解析。

```
/**
 * <pre>
 * attribute_info {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u1 info[attribute_length];
 * }
 * </pre>
 */
class AttributeInfo implements Parsable {
    private int nameIndex;  //u2
    private int attributeLength; //u4
    private Info mInfo;

    @Override
    public void parse(byte[] bytes, int offset) {
        nameIndex = Utils.read2Number(bytes, offset);
        offset += 2;
        attributeLength = Utils.read4Number(bytes, offset);
        offset += 4;
        // 根据属性名称找到匹配的属性
        ConstantItem constantItem = BytecodeParser.constantItemHashMap.get(nameIndex);
        UTF8 utf8 = (UTF8) constantItem;
        String infoName = utf8.value;
        mInfo = Info.getMatchInfo(infoName);
        if (mInfo == null) {
            System.err.println("Not found matching Attributes: " + infoName);
            return;
        }
        mInfo.parse(bytes, offset);
    }

    public int size() {
        return 2 + 4 + attributeLength;
    }

    @Override
    public String toString() {
        return "AttributeInfo{" +
                "nameIndex=" + nameIndex +
                ", attributeLength=" + attributeLength +
                ", mInfo=" + mInfo +
                '}';
    }
}
```

```
abstract class Info implements Parsable {
    public int size() {
        return contentSize();
    }

    abstract protected int contentSize();

    abstract public void parseInner(byte[] bytes, int offset);

    @Override
    public void parse(byte[] bytes, int offset) {
        parseInner(bytes, offset);
    }

    @Nullable
    public static Info getMatchInfo(String name) {
        switch (name) {
            case "Code":
                return new CodeInfo();
            case "ConstantValue":
                return new ConstantValue();
            case "Exceptions":
                return new Exceptions();
            case "LineNumberTable":
                return new LineNumberTable();
            case "LocalVariableTable":
                return new LocalVariableTable();
            case "LocalVariableTypeTable":
                return new LocalVariableTypeTable();
            case "SourceFile":
                return new SourceFile();
            case "InnerClasses":
                return new InnerClasses();
            case "Deprecated":
                return new Deprecated();
            case "Synthetic":
                return new Synthetic();
            case "StackMapTable":
                // TODO StackMapTable 待实现
                return null;
            case "Signature":
                return new Signature();
            default:
                return null;
        }
    }
}
```

#### Code 属性

```java
//region Info
/**
 * <pre>
 * Code_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 max_stack;
 *     u2 max_locals;
 *     u4 code_length;
 *     u1 code[code_length];
 *     u2 exception_table_length;
 *     {   u2 start_pc;
 *         u2 end_pc;
 *         u2 handler_pc;
 *         u2 catch_type;
 *     } exception_table[exception_table_length];
 *     u2 attributes_count;
 *     attribute_info attributes[attributes_count];
 * }
 * </pre>
 */
class CodeInfo extends Info {

    class ExceptionTable implements Parsable {
        int startPc; //u2
        int endPc; //u2
        int handlePc; // u2
        int catchType; //u2

        public int size() {
            return 2 + 2 + 2 + 2;
        }

        @Override
        public void parse(byte[] bytes, int offset) {
            startPc = Utils.read2Number(bytes, offset);
            offset += 2;
            endPc = Utils.read2Number(bytes, offset);
            offset += 2;
            handlePc = Utils.read2Number(bytes, offset);
            offset += 2;
            catchType = Utils.read2Number(bytes, offset);
        }

        @Override
        public String toString() {
            return "ExceptionTable{" +
                    "startPc=" + startPc +
                    ", endPc=" + endPc +
                    ", handlePc=" + handlePc +
                    ", catchType=" + catchType +
                    '}';
        }
    }

    int maxStack; //u2
    int maxLocals; //u2
    int codeLength; //u4
    int[] code; // u1[codeLength]
    int exceptionTableLength; //u2
    ExceptionTable[] exceptionTable;
    int attributeCount; //u2
    AttributeInfo[] attributes;

    private int attributesSize;

    @Override
    public int contentSize() {
        return attributesSize + 2 + 2 + 4 + codeLength + exceptionTableLength;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {
        maxStack = Utils.read2Number(bytes, offset);
        offset += 2;
        maxLocals = Utils.read2Number(bytes, offset);
        offset += 2;
        codeLength = Utils.read4Number(bytes, offset);
        offset += 4;
        code = new int[codeLength];
        for (int i = 0; i < codeLength; i++) {
            code[i] = Utils.readUnsignedByte(bytes, offset);
            offset += 1;
        }
        exceptionTableLength = Utils.read2Number(bytes, offset);
        offset += 2;
        exceptionTable = new ExceptionTable[exceptionTableLength];
        for (int i = 0; i < exceptionTableLength; i++) {
            exceptionTable[i] = new ExceptionTable();
            exceptionTable[i].parse(bytes, offset);
            offset += exceptionTable[i].size();
        }
        attributeCount = Utils.read2Number(bytes, offset);
        offset += 2;
        attributes = new AttributeInfo[attributeCount];
        for (int i = 0; i < attributeCount; i++) {
            attributes[i] = new AttributeInfo();
            attributes[i].parse(bytes, offset);
            offset += attributes[i].size();
            attributesSize += attributes[i].size();
        }
    }

    public String toString() {
        return "CodeInfo{" +
                "maxStack=" + maxStack +
                ", maxLocals=" + maxLocals +
                ", codeLength=" + codeLength +
                ", code=" + Arrays.toString(code) +
                ", exceptionTableLength=" + exceptionTableLength +
                ", exceptionTable=" + Arrays.toString(exceptionTable) +
                ", attributeCount=" + attributeCount +
                ", attributes=" + Arrays.toString(attributes) +
                '}';
    }
}
```

#### 常量属性

```java
class ConstantValue extends Info {
    int constantValueIndex; //u2

    @Override
    public void parseInner(byte[] bytes, int offset) {
        constantValueIndex = Utils.read2Number(bytes, offset + 2);
    }

    @Override
    public int contentSize() {
        return 2;
    }

    @Override
    public String toString() {
        return "ConstantValue{" +
                "constantValueIndex=" + constantValueIndex +
                '}';
    }
}
```

#### 异常属性

```java
/**
 * <pre>
 * Exceptions_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 number_of_exceptions;
 *     u2 exception_index_table[number_of_exceptions];
 * }
 * </pre>
 */
class Exceptions extends Info {
    int numberOfExceptions; //u2
    int[] exceptionIndexTable; //u2[numberOfExceptions]

    @Override
    public int contentSize() {
        return 2;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {
        numberOfExceptions = Utils.read2Number(bytes, offset);
        offset += 2;
        exceptionIndexTable = new int[numberOfExceptions];
        for (int i = 0; i < numberOfExceptions; i++) {
            exceptionIndexTable[i] = Utils.read2Number(bytes, offset);
            offset += 2;
        }
    }

    @Override
    public String toString() {
        return "Exceptions{" +
                "numberOfExceptions=" + numberOfExceptions +
                ", exceptionIndexTable=" + Arrays.toString(exceptionIndexTable) +
                '}';
    }
}
```

#### 字节码与源码行号对应属性

```java
/**
 * <pre>
 * LineNumberTable_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 line_number_table_length;
 *     {   u2 start_pc;
 *         u2 line_number;
 *     } line_number_table[line_number_table_length];
 * }
 * </pre>
 */
class LineNumberTable extends Info {

    class LineNumberInfo implements Parsable {
        int startPc; //u2
        int lineNumber; //u2

        public static final int size = 4;

        @Override
        public void parse(byte[] bytes, int offset) {
            startPc = Utils.read2Number(bytes, offset);
            offset += 2;
            lineNumber = Utils.read2Number(bytes, offset);
        }

        @Override
        public String toString() {
            return "LineNumberInfo{" +
                    "startPc=" + startPc +
                    ", lineNumber=" + lineNumber +
                    '}';
        }
    }

    int lineNumberTableLength; //u2
    private LineNumberInfo[] mLineNumberInfos;

    @Override
    protected int contentSize() {
        return mLineNumberInfos.length * LineNumberInfo.size + 2;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {
        lineNumberTableLength = Utils.read2Number(bytes, offset);
        offset += 2;
        mLineNumberInfos = new LineNumberInfo[lineNumberTableLength];
        for (int i = 0; i < lineNumberTableLength; i++) {
            mLineNumberInfos[i] = new LineNumberInfo();
            mLineNumberInfos[i].parse(bytes, offset);
            offset += LineNumberInfo.size;
        }
    }

    @Override
    public String toString() {
        return "LineNumberTable{" +
                "lineNumberTableLength=" + lineNumberTableLength +
                ", mLineNumberInfos=" + Arrays.toString(mLineNumberInfos) +
                '}';
    }
}
```

#### 局部变量属性

```java
/**
 * <pre>
 * LocalVariableTable_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 local_variable_table_length;
 *     {   u2 start_pc;
 *         u2 length;
 *         u2 name_index;
 *         u2 descriptor_index;
 *         u2 index;
 *     } local_variable_table[local_variable_table_length];
 * }
 * </pre>
 */
class LocalVariableTable extends Info {

    class LocalVairableTableItem implements Parsable {
        int startPc;
        int length;
        int nameIndex;
        int descriptorIndex;
        int index;

        public static final int size = 10;

        @Override
        public void parse(byte[] bytes, int offset) {
            startPc = Utils.read2Number(bytes, offset);
            length = Utils.read2Number(bytes, offset + 2);
            nameIndex = Utils.read2Number(bytes, offset + 2);
            descriptorIndex = Utils.read2Number(bytes, offset + 4);
            index = Utils.read2Number(bytes, offset + 6);
        }

        @Override
        public String toString() {
            return "LocalVairableTableItem{" +
                    "startPc=" + startPc +
                    ", length=" + length +
                    ", nameIndex=" + nameIndex +
                    ", descriptorIndex=" + descriptorIndex +
                    ", index=" + index +
                    '}';
        }
    }

    int localVariableTableLength; //u2
    private LocalVairableTableItem[] items;

    @Override
    protected int contentSize() {
        return localVariableTableLength * LocalVairableTableItem.size + 2;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {
        localVariableTableLength = Utils.read2Number(bytes, offset);
        offset += 2;
        items = new LocalVairableTableItem[localVariableTableLength];
        for (int i = 0; i < localVariableTableLength; i++) {
            items[i] = new LocalVairableTableItem();
            items[i].parse(bytes, offset);
            offset += LocalVairableTableItem.size;
        }
    }

    @Override
    public String toString() {
        return "LocalVariableTable{" +
                "localVariableTableLength=" + localVariableTableLength +
                ", items=" + Arrays.toString(items) +
                '}';
    }
}
```
#### 源代码文件属性

```java
/**
 * <pre>
 * SourceFile_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 sourcefile_index;
 * }
 * </pre>
 */
class SourceFile extends Info {

    int sourceFileIndex; //u2

    @Override
    protected int contentSize() {
        return 2;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {
        sourceFileIndex = Utils.read2Number(bytes, offset);
    }

    @Override
    public String toString() {
        return "SourceFile{" +
                "sourceFileIndex=" + sourceFileIndex + " -> " + ((UTF8) BytecodeParser.constantItemHashMap.get(sourceFileIndex)).value +
                '}';
    }
}
```

#### 内部类属性

```java
/**
 * <pre>
 * InnerClasses_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 number_of_classes;
 *     {   u2 inner_class_info_index;
 *         u2 outer_class_info_index;
 *         u2 inner_name_index;
 *         u2 inner_class_access_flags;
 *     } classes[number_of_classes];
 * }
 * </pre>
 */
class InnerClasses extends Info {

    class Classes implements Parsable {
        int innerClassInfoIndex, outerClassInfoIndex, innerNameInex, innerClassAccessFlags; //u2

        public static final int size = 8;

        @Override
        public void parse(byte[] bytes, int offset) {
            innerClassInfoIndex = Utils.read2Number(bytes, offset);
            offset += 2;
            outerClassInfoIndex = Utils.read2Number(bytes, offset);
            offset += 2;
            innerNameInex = Utils.read2Number(bytes, offset);
            offset += 2;
            innerClassAccessFlags = Utils.read2Number(bytes, offset);
        }

        @Override
        public String toString() {
            return "Classes{" +
                    "innerClassInfoIndex=" + innerClassInfoIndex +
                    ", outerClassInfoIndex=" + outerClassInfoIndex +
                    ", innerNameInex=" + innerNameInex + " -> " + ((UTF8) BytecodeParser.constantItemHashMap.get(innerNameInex)).value +
                    ", innerClassAccessFlags=" + innerClassAccessFlags + " -> " + AccessFlags.printAccess(innerClassAccessFlags) +
                    '}';
        }
    }

    int numberOfClasses; //u2
    private Classes[] mClasses;

    @Override
    protected int contentSize() {
        return numberOfClasses * Classes.size + 2;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {
        numberOfClasses = Utils.read2Number(bytes, offset);
        offset += 2;
        mClasses = new Classes[numberOfClasses];
        for (int i = 0; i < numberOfClasses; i++) {
            mClasses[i] = new Classes();
            mClasses[i].parse(bytes, offset);
            offset += Classes.size;
        }
    }

    @Override
    public String toString() {
        return "InnerClasses{" +
                "numberOfClasses=" + numberOfClasses +
                ", mClasses=" + Arrays.toString(mClasses) +
                '}';
    }
}
```

#### 泛型签名属性

```java
/**
 * <pre>
 * Signature_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 signature_index;
 * }
 * </pre>
 */
class Signature extends Info {

    int signatureIndex; //u2

    @Override
    protected int contentSize() {
        return 2;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {
        signatureIndex = Utils.read2Number(bytes, offset);
    }

    @Override
    public String toString() {
        ConstantItem constantItem = BytecodeParser.constantItemHashMap.get(signatureIndex);
        String signature = constantItem.toString();
        if (constantItem instanceof UTF8) {
            signature = ((UTF8) constantItem).value;
        } else if (constantItem instanceof METHOD_REF) {
            signature = constantItem.toString();
        } else if (constantItem instanceof STRING) {
            signature = ((UTF8) BytecodeParser.constantItemHashMap.get(((STRING) constantItem).index)).value;
        }
        return "Signature{" +
                "signatureIndex=" + signatureIndex + " -> " + signature +
                '}';
    }
}
```

#### StackMapTable 属性

StackMapTable 用于优化类型检查效率的。https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.4

```java
/**
 * https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.4
 * <pre>
 * StackMapTable_attribute {
 *     u2              attribute_name_index;
 *     u4              attribute_length;
 *     u2              number_of_entries;
 *     stack_map_frame entries[number_of_entries];
 * }
 *
 * union stack_map_frame {
 *     same_frame;
 *     same_locals_1_stack_item_frame;
 *     same_locals_1_stack_item_frame_extended;
 *     chop_frame;
 *     same_frame_extended;
 *     append_frame;
 *     full_frame;
 * }
 * </pre>
 */
class StackMapTable extends Info {

    class StackMapFrame {
        /**
         * <pre>
         * same_frame {
         *     u1 frame_type = SAME; // 0-63
         * }
         * </pre>
         */
        class SameFrame {
            int frame_type; //u1
        }

        /**
         * <pre>
         * same_locals_1_stack_item_frame {
         *     u1 frame_type = SAME_LOCALS_1_STACK_ITEM; // 64-127
         *     verification_type_info stack[1];
         * }
         * </pre>
         */
        class SameLocals1StackItemFrame {
        }

        class SameLocals1StackItemFrameExtended {
        }

        class ChopFrame {
        }

        class SameFrameExtended {
        }

        class AppendFrame {
        }

        class FullFrame {
        }
    }

    int numberOfEntries; //u2

    private StackMapFrame[] mStackMapFrames;

    @Override
    protected int contentSize() {
        return 0;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {

    }
}
```

#### Synthetic 编译器合成属性

```java
/**
 * <pre>
 * Synthetic_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length; //always zero
 * }
 * </pre>
 */
class Synthetic extends Info {

    @Override
    protected int contentSize() {
        return 0;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {

    }
}
```

#### Deprecated 属性

```java
/**
 * <pre>
 * Deprecated_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 * }
 * </pre>
 */
class Deprecated extends Info {
    @Override
    protected int contentSize() {
        return 0;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {

    }
}
```
#### LocalVariableTypeTable

局部变量类型属性

```java
/**
 * <pre>
 *  LocalVariableTypeTable_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 local_variable_type_table_length;
 *     {   u2 start_pc;
 *         u2 length;
 *         u2 name_index;
 *         u2 signature_index;
 *         u2 index;
 *     } local_variable_type_table[local_variable_type_table_length];
 * }
 * </pre>
 */
class LocalVariableTypeTable extends Info {

    class Local_variable_type_table implements Parsable {
        int start_pc;
        int length;
        int name_index;
        int signature_index;
        int index;

        public static final int size = 10;

        @Override
        public void parse(byte[] bytes, int offset) {
            start_pc = Utils.read2Number(bytes, offset);
            length = Utils.read2Number(bytes, offset + 2);
            name_index = Utils.read2Number(bytes, offset + 4);
            signature_index = Utils.read2Number(bytes, offset + 6);
            index = Utils.read2Number(bytes, offset + 8);
        }

        @Override
        public String toString() {
            return "Local_variable_type_table{" +
                    "start_pc=" + start_pc +
                    ", length=" + length +
                    ", name_index=" + name_index +
                    ", signature_index=" + signature_index +
                    ", index=" + index +
                    '}';
        }
    }

    int lvtt_length; //u2
    Local_variable_type_table[] mTables;

    @Override
    protected int contentSize() {
        return lvtt_length * Local_variable_type_table.size + 2;
    }

    @Override
    public void parseInner(byte[] bytes, int offset) {
        lvtt_length = Utils.read2Number(bytes, offset);
        offset += 2;
        mTables = new Local_variable_type_table[lvtt_length];
        for (int i = 0; i < lvtt_length; i++) {
            mTables[i] = new Local_variable_type_table();
            mTables[i].parse(bytes, offset);
            offset += Local_variable_type_table.size;
        }
    }

    @Override
    public String toString() {
        return "LocalVariableTypeTable{" +
                "lvtt_length=" + lvtt_length +
                ", mTables=" + Arrays.toString(mTables) +
                '}';
    }
}

//endregion
```

## 从字节中获取数字的工具类

由于 Java 的所有数字类型都是 signed 类型，就会导致 unsigned 的数到了 Java 中可能越界溢出。所以统一使用 Java 的 `int` 代表 `unsigned byte`。

```java
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
```
