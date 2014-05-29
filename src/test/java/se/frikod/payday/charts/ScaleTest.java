package se.frikod.payday.charts;

import android.test.InstrumentationTestCase;


import java.lang.Exception;

public class ScaleTest extends InstrumentationTestCase{

    public void test() throws Exception {
        final int expected = 1;
        final int reality = 5;
        assertEquals(expected, reality);
    }

	public void testPositive() {
		Scale s = new Scale();
        assert true;
		s.update(0, 10, 0, 100);
		assert s.apply(5) == 50;

		s.update(-10, 10, 0, 100);
		assert s.apply(0) == 50;
	}

}