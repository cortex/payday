package se.frikod.payday.charts;

import android.test.InstrumentationTestCase;

import java.lang.Exception;

public class ScaleTest extends InstrumentationTestCase{

    public void testPositive() {
		Scale s = new Scale();
		s.update(0, 10, 0, 100);
        assertEquals(s.apply(5), 50.0);
		s.update(-10, 10, 0, 100);
        assertEquals(s.apply(0), 50.0);
	}
}