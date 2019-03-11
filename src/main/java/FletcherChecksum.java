

/**
 * A simple checksum algorithm.
 * Can be used with both integers and floats (floats are converted to integers, as they are: 4 bytes to 4 bytes).
 * <p> 
 * See: <br>
 * https://en.wikipedia.org/wiki/Fletcher's_checksum
 * 
 * @author Betalord
 */
public class FletcherChecksum {
	private int f1 = 0;
	private int f2 = 0;
	
	public void add(int block) {
		f1 += block;
		f2 += f1;
	}
	
	public void add(float block) {
		add(Float.floatToIntBits(block)); // https://docs.oracle.com/javase/7/docs/api/java/lang/Float.html
	}

	public void add(String block) {
		for (byte b : block.getBytes())
			add(b);
	}
	
	public long getChecksum() {
		return f1 + (f2 << 32);
	}
	
}
