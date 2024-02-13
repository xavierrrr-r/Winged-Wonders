package za.co.varsitycollege.opsc7312poe.myapplication
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitService {

    private const val BASE_URL = "https://ebird.org/ws2.0/" // Base URL of the eBird API

    fun createEBirdApiService(apiKey: String): eBirdAPIs {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.ebird.org/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(eBirdAPIs::class.java)
    }
}