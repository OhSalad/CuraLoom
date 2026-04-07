package com.pmu.mobileapp.model

class Episode {
    var id: Long = 0
    var feedId: Long = 0
    var title: String = ""
    var description: String? = null
    var pubDate: String? = null
    var duration: String? = null
    var audioUrl: String? = null
    var isPlayed: Boolean = false
    var isDownloaded: Boolean = false
    var isNew: Boolean = true

    constructor()

    constructor(feedId: Long, title: String, description: String?, pubDate: String?, duration: String?) {
        this.feedId = feedId
        this.title = title
        this.description = description
        this.pubDate = pubDate
        this.duration = duration
        this.isNew = true
    }
}
