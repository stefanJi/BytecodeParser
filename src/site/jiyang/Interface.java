package site.jiyang;


import java.util.Arrays;

class Interfaces extends Section {

    int interfaceCount;
    int[] indexs;

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
