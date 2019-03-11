/**
 * "Number on screen". Used with number detection algorithm (when detecting selected difficulty level, for example).
 * 
 * @author Betalord
 */
public class ScreenNum implements Comparable<ScreenNum> {
	int value; // [0..9]
	int xpos; // horizontal position on screen (relative to the detected image, for example)
	
	public ScreenNum() { }
	
	public ScreenNum(int value, int xpos) {
		this.value = value;
		this.xpos = xpos;
	}
	
	public int compareTo(ScreenNum other) {
		return Integer.compare(this.xpos, other.xpos);
	}
}
