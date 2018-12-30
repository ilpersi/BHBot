import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.imageio.ImageIO;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 
 * @author Betalord
 */
public class Misc {
	// https://stackoverflow.com/questions/1205135/how-to-encrypt-string-in-java
    public static final String DEFAULT_ENCODING = "UTF-8"; 
    static BASE64Encoder enc = new BASE64Encoder();
    static BASE64Decoder dec = new BASE64Decoder();

    public static String base64encode(String text) {
        try {
            return enc.encode(text.getBytes(DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }//base64encode

    public static String base64decode(String text) {
        try {
            return new String(dec.decodeBuffer(text), DEFAULT_ENCODING);
        } catch (IOException e) {
            return null;
        }
    }//base64decode
    
    public static String xorMessage(String message, String key) {
        try {
            if (message == null || key == null) return null;

            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();

            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];

            for (int i = 0; i < ml; i++) {
                newmsg[i] = (char)(mesg[i] ^ keys[i % kl]);
            }//for i

            return new String(newmsg);
        } catch (Exception e) {
            return null;
        }
    }//xorMessage
    
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("<HH:mm:ss>");
	
	public static final DecimalFormat num4Digits = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num3Digits = new DecimalFormat("#.###", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num2Digits = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num1Digit = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
	
	public static void log(String s) {
		System.out.println("<" + dateFormat.format(new Date()) + "> " + s);
	}

	/**
	 * Return time in milliseconds from the start of the system. Can have a negative value. 
	 */
	public static long getTime() {
		return System.nanoTime() / 1000000;
	}
	
	public static String getStackTrace() {
		String r = "";

		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			r += ste + "\n";
		}

		return r;
	}

	public static String readTextFile(String file) {
		return readTextFile(file, false);
	}
	// http://stackoverflow.com/questions/4716503/reading-a-plain-text-file-in-java
	public static String readTextFile(String file, boolean ignoreErrors) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			try {
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();
				while (line != null) {
					sb.append(line);
					sb.append(System.lineSeparator());
					line = br.readLine();
				}
				String everything = sb.toString();
				return everything;
			} finally {
				br.close();
			}			
		} catch (IOException e) {
			if (!ignoreErrors)
				e.printStackTrace();
			return "";
		}
	}
	
	public static List<String> readTextFile2(String file) {
		List<String> lines = new ArrayList<String>();
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
			e.printStackTrace();
			return null;
		}
	}

	/** Returns true on success. */
	public static boolean saveTextFile(String file, String contents) {
		return saveTextFile(file, contents, false);
	}
	
	/** Returns true on success. */
	public static boolean saveTextFile(String file, String contents, boolean ignoreErrors) {
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
			if (!ignoreErrors)
				e.printStackTrace();
			return false;
		}
		return true;
	}
	
	// https://stackoverflow.com/questions/1383797/java-hashmap-how-to-get-key-from-value
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (Objects.equals(value, entry.getValue())) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}
	
	public static String millisToHumanForm(int millis) {
		int s = millis / 1000;
		int m = s / 60;
		s = s % 60;
		int h = m / 60;
		m = m % 60;
		
		if (s==0 && m==0 && h==0)
			return "0s";
		
		return (h > 0 ? (h + "h") : "") + (m > 0 ? (m + "m") : "") + (s > 0 ? (s + "s") : "");
	}
	
	public static int max(int... values) {
		int max = Integer.MIN_VALUE;
		for (int value : values)
			if (value > max)
				max = value;
		return max;
	}
	
	public static int min(int... values) {
		int min = Integer.MAX_VALUE;
		for (int value : values)
			if (value < min)
				min = value;
		return min;
	}
	
	public static boolean saveImage(BufferedImage img, String file) {
		try {
		    // retrieve image
		    File outputfile = new File(file);
		    ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
		    return false;
		}
		return true;
	}
	
	/**
	 * Returns index of closest match from the 'values' array.
	 */
	public static int findClosestMatch(int[] values, int value) {
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
	
	public static String listToString(List<String> list) {
		String r = "";
		for (String s : list)
			r += s + ", ";
		if (r.length() > 0)
			r = r.substring(0, r.length()-2); // remove last ", "
		return r;
	}

	public static String listToString(EnumSet<?> list) {
		String r = "";
		for (Object e : list)
			r += e + ", ";
		r = r .substring(0, r.length()-2);
		return r;
	}
	
	/** 
	 * https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
	 * @return path to the exe (without an exe). Example: "/E:/Eclipse/workspace/BHBot/bin/".
	 */
	public static String getExePath() {
		try {
			return URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource(".").getPath(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		/*
		try {
			return (new File(Misc.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())).getAbsolutePath();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		*/
	}
	
}
