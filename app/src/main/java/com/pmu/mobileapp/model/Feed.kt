package com.pmu.mobileapp.model

class Feed {
    var id: Long = 0
    var title: String = ""
    var url: String = ""
    var description: String? = null
    var category: String? = null
    var imageUrl: String? = null
    var author: String? = null
    var isNew: Boolean = true
    var addedAt: String? = null
    var lastRefreshed: String? = null

    constructor()

    constructor(title: String, url: String, description: String?, category: String?, author: String?) {
        this.title = title
        this.url = url
        this.description = description
        this.category = category
        this.author = author
        this.isNew = true
    }
}
