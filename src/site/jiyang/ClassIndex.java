package site.jiyang;


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
