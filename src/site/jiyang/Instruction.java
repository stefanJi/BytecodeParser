package site.jiyang;


// region Instruction

import java.util.Arrays;

/**
 * <pre>
 * mnemonic //指令助记符, 以数字编号存储
 * operand1 //操作数1
 * operand2 //操作数2
 * ...      //每个指令的操作数个数是固定的
 * 详见: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.4
 * </pre>
 */
abstract class Instruction implements Parsable {
    abstract public String name();

    abstract public int[] values();

    public int size() {
        return 1 /* u1 opcode*/ + values().length /* 每个操作数类型为 u1 */;
    }

    @Override
    public String toString() {
        return String.format("Instruction{%s %s}", name(), Arrays.toString(values()));
    }

    static Instruction findInstructionByOpcode(int opcode) {
        switch (opcode) {
            case 18:
                return new LDC();
            case 50:
            case 83:
            case 1:
            case 25:
            case 42:
            case 43:
            case 44:
            case 45:
            case 189:
            case 176:
            case 190:
            case 58:
            case 75:
            case 76:
            case 77:
            case 78:
            case 191:
            case 51:
            case 84:
                // 还有很多指令 ...
            default:
                //TODO 实现其他字节码指令
                return null;
        }
    }
}

/**
 * ldc: 从运行时常量池中取出一个值，然后入栈到操作数栈
 * <pre>
 * ldc = 18 (0x12)
 * </pre>
 */
class LDC extends Instruction {

    int index; //u1 常量池中的索引

    @Override
    public void parse(byte[] bytes, int offset) {
        index = Utils.readUnsignedByte(bytes, offset);
    }

    @Override
    public String name() {
        return "ldc";
    }

    @Override
    public int[] values() {
        return new int[]{index};
    }
}

//endregion
