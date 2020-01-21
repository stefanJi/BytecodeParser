package site.jiyang;

import java.util.ArrayList;
import java.util.Arrays;

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

    private ArrayList<Instruction> mInstructions = new ArrayList<>(); // store code to instruction

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

        int step = 0;
        for (int i = 0; i < code.length; ) {
            int opcode = code[i];
            Instruction instruction = Instruction.findInstructionByOpcode(opcode);
            if (instruction == null) {
                break;
            }
            instruction.parse(bytes, offset + step);
            mInstructions.add(instruction);
            step = instruction.size();
            i += step;
        }
    }

    @Override
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
                ", mInstructions=" + mInstructions +
                ", attributesSize=" + attributesSize +
                '}';
    }
}

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
