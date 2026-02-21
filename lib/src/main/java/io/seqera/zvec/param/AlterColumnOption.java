package io.seqera.zvec.param;

public class AlterColumnOption {
    private int concurrency = 0;

    public AlterColumnOption() {}

    public AlterColumnOption concurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public int concurrency() { return concurrency; }
}
