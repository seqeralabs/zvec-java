package io.seqera.zvec.param;

public class AddColumnOption {
    private int concurrency = 0;

    public AddColumnOption() {}

    public AddColumnOption concurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public int concurrency() { return concurrency; }
}
