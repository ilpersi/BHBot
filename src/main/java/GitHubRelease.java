import com.google.gson.annotations.SerializedName;


class GitHubRelease {

    @SerializedName(value="tag_name")
    String tagName;

    @SerializedName(value="body")
    String releaseNotes;

    @SerializedName(value="html_url")
    String releaseURL;

}
