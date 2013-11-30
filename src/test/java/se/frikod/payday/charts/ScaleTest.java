package se.frikod.payday.charts;

import org.junit.Test;

public class ScaleTest {
	@Test
	public void testInstantiation() {
		new Scale();
	}

	@Test
	public void testPositive() {
		Scale s = new Scale();

		s.update(0, 10, 0, 100);
		assert s.apply(5) == 50;

		s.update(-10, 10, 0, 100);
		assert s.apply(0) == 50;
	}

}