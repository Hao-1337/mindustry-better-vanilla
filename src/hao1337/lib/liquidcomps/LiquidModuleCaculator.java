package hao1337.lib.liquidcomps;

import mindustry.type.Liquid;
import static mindustry.Vars.*;

import java.util.Arrays;

import arc.math.WindowedMean;
import arc.struct.Bits;
import arc.util.Interval;
import arc.util.Nullable;

public class LiquidModuleCaculator {
    private final int windowSize = 6;
    private final Interval flowTimer = new Interval(2);
    private final float pollScl = 10f;

    private float[] cacheSums;
    private float[] displayFlow;
    private final Bits cacheBits = new Bits();

    private float[] liquids = new float[content.liquids().size];
    private @Nullable WindowedMean[] flow;

    public LiquidModuleCaculator() { Arrays.fill(liquids, 0); }

    public void set(Liquid liq, float am) {
        if (flow != null) {
            float nv = am - liquids[liq.id];
            if (nv > 0f) cacheSums[liq.id] += nv;
        }
        liquids[liq.id] = am;
    }

    void initFlow() {
        if (flow != null) return;

        flow = new WindowedMean[liquids.length];
        cacheSums = new float[liquids.length];
        displayFlow = new float[liquids.length];

        for (int i = 0; i < liquids.length; i++) {
            flow[i] = new WindowedMean(windowSize);
            displayFlow[i] = -1f;
        }
    }

    public void updateFlow() {
        if (flowTimer.get(1, pollScl)) {
            initFlow();
            boolean updateFlow = flowTimer.get(15);

            for (int i = 0; i < liquids.length; i++) {
                flow[i].add(cacheSums[i]);
                if (cacheSums[i] != 0f) cacheBits.set(i);
                cacheSums[i] = 0;

                if (updateFlow) {
                    displayFlow[i] = flow[i].hasEnoughData() ? flow[i].mean() / pollScl : -1;
                }
            }
        }
    }

    public float getFlowRate(Liquid liquid) { return flow == null ? -1f : displayFlow[liquid.id]; }
    public boolean hasFlowLiquid(Liquid liquid) { return flow != null && cacheBits.get(liquid.id); }

    public float sum(LiquidCalculator calc) {
        float sum = 0f;
        for (int i = 0; i < liquids.length; i++) {
            if (liquids[i] > 0) {
                sum += calc.get(content.liquid(i), liquids[i]);
            }
        }
        return sum;
    }

    public void checkArrayCapacity(int size) {
        if (liquids.length != size)
            liquids = Arrays.copyOf(liquids, size);
    }

    public interface LiquidCalculator {
        float get(Liquid liquid, float amount);
    }
}
