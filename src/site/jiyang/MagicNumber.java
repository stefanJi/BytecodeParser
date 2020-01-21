package site.jiyang;


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
