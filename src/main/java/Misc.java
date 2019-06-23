import com.google.common.base.Throwables;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 
 * @author Betalord
 */
public class Misc {

	/**
	 * Return time in milliseconds from the start of the system. Can have a negative value. 
	 */
	static long getTime() {
		return System.nanoTime() / 1000000;
	}
	
	static String getStackTrace() {
		StringBuilder r = new StringBuilder();

		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			r.append(ste).append("\n");
		}

		return r.toString();
	}

	static List<String> readTextFile2(String file) {
		List<String> lines = new ArrayList<>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			try {
				String line = br.readLine();
				while (line != null) {
					lines.add(line);
					line = br.readLine();
				}
				return lines;
			} finally {
				br.close();
			}			
		} catch (IOException e) {
			BHBot.logger.error("Impossible to read file: " + file, e);
			return null;
		}
	}

	/** Returns true on success. */
	static boolean saveTextFile(String file, String contents) {
		return saveTextFile(file, contents, false);
	}
	
	/** Returns true on success. */
	private static boolean saveTextFile(String file, String contents, boolean ignoreErrors) {
		BufferedWriter bw;
		
		try {
			File f = new File(file);
			// create parent folder(s) if needed:
			File parent = f.getParentFile();
			if (parent != null && !parent.exists())
				parent.mkdirs(); 
			
			bw = new BufferedWriter(new FileWriter(f));
			try {
				bw.write(contents);
			} finally {
				bw.close();
			}			
		} catch (IOException e) {
			if (!ignoreErrors) {
				BHBot.logger.error("saveTextFile could not save contents in file: " + file, e);
			}
			return false;
		}
		return true;
	}
	
	static String millisToHumanForm(int millis) {
		int s = millis / 1000;
		int m = s / 60;
		s = s % 60;
		int h = m / 60;
		m = m % 60;
		
		if (s==0 && m==0 && h==0)
			return "0s";
		
		return (h > 0 ? (h + "h") : "") + (m > 0 ? (m + "m") : "") + (s > 0 ? (s + "s") : "");
	}
	
	static int max(int... values) {
		int max = Integer.MIN_VALUE;
		for (int value : values)
			if (value > max)
				max = value;
		return max;
	}
	
	static int min(int... values) {
		int min = Integer.MAX_VALUE;
		for (int value : values)
			if (value < min)
				min = value;
		return min;
	}
	
	/**
	 * Returns index of closest match from the 'values' array.
	 */
	static int findClosestMatch(int[] values, int value) {
		int best = Integer.MAX_VALUE;
		int bestIndex = -1;
		for (int i = 0; i < values.length; i++) {
			if (Math.abs(values[i] - value) < best) {
				best = Math.abs(values[i] - value);
				bestIndex = i;
			}
		}
		return bestIndex;
	}
	
	static String listToString(EnumSet<?> list) {
		StringBuilder r = new StringBuilder();
		for (Object e : list)
			r.append(e).append(", ");
		r = new StringBuilder(r.substring(0, r.length() - 2));
		return r.toString();
	}

	static String encodeFileToBase64Binary(File toEncode)  {

		byte[] encoded = new byte[0];
		try {
			encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(toEncode));
		} catch (IOException e) {
			BHBot.logger.error("Error in encodeFileToBase64Binary", e);
		}
		return new String(encoded, StandardCharsets.US_ASCII);
	}

	static int[][] convertTo2D(BufferedImage image) {

		final int w = image.getWidth();
		final int h = image.getHeight();

		int[][] pixels = new int[w][h];

		for( int i = 0; i < w; i++ )
			for( int j = 0; j < h; j++ )
				pixels[i][j] = image.getRGB( i, j );

		return pixels;
	}

	static long classBuildTimeMillis() throws URISyntaxException, IllegalStateException, IllegalArgumentException {
		URL resource = MainThread.class.getResource(MainThread.class.getSimpleName() + ".class");
		if (resource == null) {
			throw new IllegalStateException("Failed to find class file for class: " +
					MainThread.class.getName());
		}

		if (resource.getProtocol().equals("file")) {

			return new File(resource.toURI()).lastModified();

		} else if (resource.getProtocol().equals("jar")) {

			String path = resource.getPath();
			return new File(path.substring(5, path.indexOf("!"))).lastModified();

		} else {

			throw new IllegalArgumentException("Unhandled url protocol: " +
					resource.getProtocol() + " for class: " +
					MainThread.class.getName() + " resource: " + resource.toString());
		}
	}

	static Properties getGITInfo() {
		Properties properties = new Properties();
		try {
			InputStream gitResource = Misc.class.getClassLoader().getResourceAsStream("git.properties");
			properties.load(gitResource);
		} catch (IOException e) {
			BHBot.logger.error("Impossible to get GIT information", e);
		}
		return properties;
	}
}
