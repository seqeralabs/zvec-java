package io.seqera.zvec.param;

public class CollectionOption {
    private boolean readOnly = false;
    private boolean enableMmap = true;
    private int maxBufferSize = 64 * 1024 * 1024; // 64MB default

    public CollectionOption() {}

    public CollectionOption readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public CollectionOption enableMmap(boolean enableMmap) {
        this.enableMmap = enableMmap;
        return this;
    }

    public CollectionOption maxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
        return this;
    }

    public boolean readOnly() { return readOnly; }
    public boolean enableMmap() { return enableMmap; }
    public int maxBufferSize() { return maxBufferSize; }
}
