package com.vlm.core.data

import android.util.Log
import com.vlm.core.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object MovieBoxRepository {
    suspend fun fetchMovies(): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect("https://idflix.top/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()

            val items = doc.select(".item, .film-item, article, .movie-card")
            val scrapedList = mutableListOf<Movie>()

            for ((index, element) in items.withIndex()) {
                val titleElem = element.select(".title, h2, h3, .film-name").first()
                val imgElem = element.select("img").first()
                val linkElem = element.select("a").first()

                val title = titleElem?.text() ?: continue
                val posterUrl = imgElem?.absUrl("data-src")?.takeIf { it.isNotBlank() }
                    ?: imgElem?.absUrl("src") ?: ""
                val link = linkElem?.absUrl("href") ?: "https://idflix.top/"

                if (title.isNotBlank() && posterUrl.isNotBlank()) {
                    scrapedList.add(
                        Movie(
                            id = (index + 1).toString(),
                            title = title,
                            overview = "Stream $title for free in HD without ads or VIP restrictions on IdFlix.",
                            posterUrl = posterUrl,
                            backdropUrl = posterUrl,
                            releaseYear = "2024",
                            rating = "8.0",
                            genre = "Trending, HD",
                            watchUrl = link
                        )
                    )
                }
            }

            if (scrapedList.isNotEmpty()) {
                scrapedList
            } else {
                getFallbackMovies()
            }
        } catch (e: Exception) {
            Log.e("dsd", "${e.message}")
            getFallbackMovies()
        }
    }

    fun getFallbackMovies(): List<Movie> {
        return listOf(
            Movie(
                id = "1",
                title = "Deadpool & Wolverine",
                overview = "A listless Wade Wilson toils away in civilian life with his days as the morally flexible mercenary, Deadpool, behind him. But when his homeworld faces an existential threat, Wade must reluctantly suit up.",
                posterUrl = "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/y2ca1neKke2mGPMaHzlCDN3qsK0.jpg",
                releaseYear = "2024",
                rating = "7.8",
                genre = "Action, Comedy, Sci-Fi",
                watchUrl = "https://idflix.top/"
            ),
            Movie(
                id = "2",
                title = "Inside Out 2",
                overview = "Teenager Riley's mind headquarters is undergoing a sudden demolition to make room for something entirely unexpected: new Emotions!",
                posterUrl = "https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/xg2bc3U0LDAdwEHOhvLVAVDe61H.jpg",
                releaseYear = "2024",
                rating = "7.6",
                genre = "Animation, Family",
                watchUrl = "https://idflix.top/"
            ),
            Movie(
                id = "3",
                title = "Dune: Part Two",
                overview = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
                posterUrl = "https://image.tmdb.org/t/p/w500/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg",
                releaseYear = "2024",
                rating = "8.2",
                genre = "Sci-Fi, Adventure",
                watchUrl = "https://idflix.top/"
            ),
            Movie(
                id = "4",
                title = "Furiosa: A Mad Max Saga",
                overview = "As the world fell, young Furiosa is snatched from the Green Place of Many Mothers and falls into the hands of a great Biker Horde.",
                posterUrl = "https://image.tmdb.org/t/p/w500/iADOJ8Zymht2JPMoy3R7xceZprc.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/wNAhuOZ3Zf84jCIlrcI6JhgmY5q.jpg",
                releaseYear = "2024",
                rating = "7.5",
                genre = "Action, Sci-Fi",
                watchUrl = "https://idflix.top/"
            ),
            Movie(
                id = "5",
                title = "Alien: Romulus",
                overview = "While scavenging the deep ends of a derelict space station, a group of young space colonizers come face to face with the most terrifying life form.",
                posterUrl = "https://image.tmdb.org/t/p/w500/b33nnKl1GSFbao4l3fZDDqsMx0F.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/9SSEydLCRg8GyynALtsAeDUnHE8.jpg",
                releaseYear = "2024",
                rating = "7.3",
                genre = "Horror, Sci-Fi",
                watchUrl = "https://idflix.top/"
            ),
            Movie(
                id = "6",
                title = "Despicable Me 4",
                overview = "Gru and Lucy and their girls welcome a new member to the Gru family, Gru Jr., who is intent on making his dad's life miserable.",
                posterUrl = "https://image.tmdb.org/t/p/w500/wWba3TaojhK7NdycRhoQpsG0FaH.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/lgkPzcOSnTvjeMnuFzhzRO5HH6D.jpg",
                releaseYear = "2024",
                rating = "7.1",
                genre = "Animation, Comedy",
                watchUrl = "https://idflix.top/"
            )
        )
    }
}
