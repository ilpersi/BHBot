import java.awt.image.BufferedImage;

/**
 * 
 * @author Betalord
 */
public class Cue {
	public String name;
	public BufferedImage im;
	public Bounds bounds;
	
	public Cue() { }
	
	public Cue(String name, BufferedImage im) {
		this.name = name;
		this.im = im;
		bounds = null;
	}
	
	public Cue(String name, BufferedImage im, Bounds bounds) {
		this.name = name;
		this.im = im;
		this.bounds = bounds;
	}
	
	public Cue(Cue cue, Bounds bounds) {
		this.name = cue.name;
		this.im = cue.im;
		this.bounds = bounds;
	}
}
