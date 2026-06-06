package com.app.hihlo.model.staticModel

data class Reel(
    val id: Int,
    val title: String,
    val videoUrl: String,
    var lastPlaybackPosition: Long = 0L
)

fun getDummyReels(): List<Reel> = listOf(
    Reel(1, "Reel 1", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
    Reel(2, "Reel 2", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"),
    Reel(3, "Reel 3", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
    Reel(4, "Reel 4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"),
    Reel(5, "Reel 5", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"),
    Reel(6, "Reel 6", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
    Reel(7, "Reel 7", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4")
)



