package se.frikod.payday.charts;

/**
 * Created by joakim on 11/28/13.
 */

public class Scale{
    private double k;
    private double m;

    public void update(Number inMin, Number inMax, double outMin, double outMax){
        k = (outMax - outMin) / (inMax.doubleValue() - inMin.doubleValue());
        m = outMin - k*inMin.doubleValue();
        //Log.i("Payday.Scale", String.format("inMin:%f inMax:%f", inMin.doubleValue(), inMax.doubleValue()));
        //Log.i("Payday.Scale", String.format("outMin:%f outMax:%f", outMin, outMax));
        //Log.i("Payday.Scale", String.format("k:%f m:%f", k, m));
    }

    public double apply(Number value){
        return value.doubleValue()* k + m;
    }
}
