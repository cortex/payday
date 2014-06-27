package se.frikod.payday.charts;

public class Scale{
    private double k = 1;
    private double m = 0;

    public void update(Number inMin, Number inMax, double outMin, double outMax){

        k = (outMax - outMin) / (inMax.doubleValue() - inMin.doubleValue());
        m = outMin - k * inMin.doubleValue();
    }

    public double apply(Number value){
        return value.doubleValue() * k + m;
    }
    public double unscale(Number value){
        return (value.doubleValue() - m) / k;
    }

    public String toString(){
        return String.format("Scale k: %s m: %s", k, m);
    }
}
