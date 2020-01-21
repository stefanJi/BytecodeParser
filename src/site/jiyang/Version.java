package site.jiyang;


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
