package se.frikod.payday.charts;

public class Scale{
    private double k;
    private double m;

    public void update(Number inMin, Number inMax, double outMin, double outMax){
        k = (outMax - outMin) / (inMax.doubleValue() - inMin.doubleValue());
        m = outMin - k*inMin.doubleValue();
    }

    public double apply(Number value){
        return value.doubleValue()* k + m;
    }
}
