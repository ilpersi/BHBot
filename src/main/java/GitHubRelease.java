import com.google.gson.annotations.SerializedName;

class GitHubTagObject {

    @SerializedName("sha")
    String sha;

    /*@SerializedName("type")
    public String type;

    @SerializedName("url")
    public String url;*/

}

class GitHubTag {

    /*@SerializedName("ref")
    public String ref;

    @SerializedName("node_id")
    public String nodeId;

    @SerializedName("url")
    public String url;*/

    @SerializedName("object")
    public GitHubTagObject object;

}

class GitHubRelease {

    @SerializedName(value="tag_name")
    String tagName;

    @SerializedName(value="body")
    String releaseNotes;

    @SerializedName(value="html_url")
    String releaseURL;

}


