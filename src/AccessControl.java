import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AccessControl {
	public static String REMOTE_ADDRESS = "http://188.230.165.245";
	public static int REMOTE_PORT = 8080;
	public static long accessTokenIssued = 0; // timestamp when this access token has been issued (we need to to detect for example user changing date of system to avoid token expiration date) 
	public static String accessToken = ""; // "meat" of the token. In this string, the token's data is encoded (which is currently something like "GRANTED [encoded expiration date]".
	public static String user = "test"; // default username
	public static final String USER_FILE = "user.txt";
	public static final String TOKEN_STORAGE = "token.dat";
	
	private static String serial; // it's a simple checksum of the program's exe path
	
	/**
	 * Must be called exactly once (at the beginning, before anything else)!
	 * Returns true in case access is granted, false otherwise.
	 */
	public static boolean check(int timeout) {
		// load user token (a username) from disk, if it exists (or else use default user token):
		String s = Misc.readTextFile(USER_FILE, true);
		if (s.trim().equals(""))
			; // will use the default username
		else {
			user = s;
			BHBot.log("User token loaded from " + USER_FILE + ": <" + user+  ">");
		}
		
		// generate serial key:
		FletcherChecksum checksum = new FletcherChecksum();
		checksum.add(Misc.getExePath());
		serial = "" + checksum.getChecksum();

		accessToken = null;
		try {
			accessToken = requestToken(timeout);
		} catch (Exception e) {
			e.printStackTrace();
			BHBot.log("Error: requesting access token failed.");
		}
		
		if (accessToken != null) {
	    	saveAccessToken(accessToken);
	    	//***BHBot.log("Access token saved: <" + accessToken + ">.");
		}
		
		loadAccessToken();
		
		// check if access token is valid:
		if (accessToken == null)
			return false;

		if (accessTokenIssued > System.currentTimeMillis()) // user might have tempered with system time
			return false;
		
		try {
			long timestamp = getExpirationTimestamp(accessToken, serial);
			if (timestamp < System.currentTimeMillis()) // token has expired!
				return false;
		} catch (Exception e) {
			return false;
		}
		
		//***BHBot.log("Access token valid until: " + new Date(getExpirationTimestamp(accessToken, serial)));
		return true;
	}

	private static void loadAccessToken() {
		accessTokenIssued = 0;
		accessToken = null;
		
		try {
			String token = Misc.xorMessage(Misc.base64decode(Misc.readTextFile(TOKEN_STORAGE, true)), "some random key");
			if (token.equals(""))
				return;
			String[] s = token.split(";");
			if (s.length != 2)
				return;
			long timeIssued = Long.parseLong(s[0]);
			//long timeExpiration = getExpirationTimestamp(s[1], serial);

			// OK now copy over to the public token:
			accessTokenIssued = timeIssued;
			accessToken = s[1];
		} catch (Exception e) {
			return ;
		}
	}
	
	private static void saveAccessToken(String token) {
		Misc.saveTextFile(TOKEN_STORAGE, Misc.base64encode(Misc.xorMessage(System.currentTimeMillis() + ";" + token, "some random key")));
	}
	
	/**
	 * Returns access token received from the server or null in case something went wrong.
	 */
	private static String requestToken(int timeout) throws Exception {
		String address = REMOTE_ADDRESS + ":" + REMOTE_PORT + "/bhbot/getaccesstoken?user=" + URLEncoder.encode(user, "UTF-8") + "&serial=" + serial;

		URL url = new URL(address);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", "Mozilla/5.0");

		// System.out.println("Sending 'GET' request to URL : " + url);
		int responseCode;
		con.setConnectTimeout(timeout);
		try {
			responseCode = con.getResponseCode();
		} catch (Exception e) {
			return null;
		}
		// System.out.println("Response Code : " + responseCode);

		if (responseCode == 404 || responseCode == 500) {
			BHBot.log("Error: unable to obtain access token (" + responseCode + ").");
			return null;
		}

		InputStream in = con.getInputStream();
		//OutputStream out = new FileOutputStream("/Users/ravikiran/Desktop/abc.jpg");
		String resp = ""; // response from the server
		try {
			byte[] bytes = new byte[2048];
			int length;

			while ((length = in.read(bytes)) != -1) {
				//out.write(bytes, 0, length);
				resp += new String(bytes, 0, length);
			}
		} finally {
			in.close();
			//out.close();
		}

		return resp;
	}
	
	private static long getExpirationTimestamp(String token, String serial) {
		final String key = "Some secret key (like a key phrase used for XOR-ing)";
		
		try {
			String[] s = Misc.xorMessage(Misc.base64decode(token), Misc.xorMessage(key, serial)).split(" ");
			if (s.length != 2)
				return 0;
			if (!s[0].equals("GRANTED"))
				return 0;
			return Long.parseLong(s[1]);
		} catch (Exception e) {
			return 0;
		}
	}
	
}
