package io.seqera.zvec.param;

public class OptimizeOption {
    private int concurrency = 0;

    public OptimizeOption() {}

    public OptimizeOption concurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public int concurrency() { return concurrency; }
}
