package myself.zch.minidouyin.JavaBeans;

import com.google.gson.annotations.SerializedName;

public class Feed {

    @SerializedName("student_id") private String student_id;
    @SerializedName("user_name") private  String user_name;
    @SerializedName("image_url") private String image_url;
    @SerializedName("video_url") private String video_url;

    public String getStudent_id() {
        return student_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public String getImage_url() {
        return image_url;
    }

    public String getVideo_url() {
        return video_url;
    }
}