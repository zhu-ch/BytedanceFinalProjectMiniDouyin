package myself.zch.minidouyin;

import myself.zch.minidouyin.JavaBeans.FeedResponse;
import myself.zch.minidouyin.JavaBeans.PostVideoResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface IMiniDouyinService {
    @Multipart
    @POST("/minidouyin/video")
    Call<PostVideoResponse>
    createVideo(
            @Query("student_id") String student_id,
            @Query("user_name") String user_name,
            @Part MultipartBody.Part cover_image,
            @Part MultipartBody.Part video
    );


    @GET("minidouyin/feed")
    Call<FeedResponse> getFeed();
}
