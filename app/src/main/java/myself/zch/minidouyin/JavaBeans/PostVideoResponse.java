package myself.zch.minidouyin.JavaBeans;

import com.google.gson.annotations.SerializedName;

public class PostVideoResponse {

    @SerializedName("item")   private Feed feed;
    @SerializedName("success")   private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public Feed getFeed() {
        return feed;
    }

}