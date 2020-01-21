package site.jiyang;

import java.util.HashMap;

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
            // 找到匹配的常量
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
