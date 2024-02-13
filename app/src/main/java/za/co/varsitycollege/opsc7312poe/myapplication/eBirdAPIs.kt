package za.co.varsitycollege.opsc7312poe.myapplication
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
interface eBirdAPIs {
    @GET("https://api.ebird.org/v2/ref/hotspot/geo?apiKey=keodjjotqkd0")
    suspend fun getHotspots(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("maxResults") maxResults: Int,
        @Query("dist") distance: Double,
        @Query("fmt") format: String = "csv",
        @Query("key") apiKey: String
    ): Response<ResponseBody> // Use Response<ResponseBody> to handle the response
}