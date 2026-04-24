package com.pmu.mobileapp.data

data class DefaultPodcastFeed(
    val title: String,
    val url: String,
    val description: String,
    val category: String,
    val author: String
)

object DefaultPodcastFeeds {
    val starterFeeds: List<DefaultPodcastFeed>
        get() = feeds.take(6)

    val feeds = listOf(
        DefaultPodcastFeed(
            title = "99% Invisible",
            url = "https://feeds.simplecast.com/BqbsxVfO",
            description = "Design, architecture, and the invisible systems shaping everyday life.",
            category = "Design",
            author = "Roman Mars"
        ),
        DefaultPodcastFeed(
            title = "The Daily",
            url = "https://feeds.simplecast.com/54nAGcIl",
            description = "A deep look at one major news story each weekday.",
            category = "News",
            author = "The New York Times"
        ),
        DefaultPodcastFeed(
            title = "Accidental Tech Podcast",
            url = "https://atp.fm/episodes?format=rss",
            description = "A detailed weekly conversation about Apple, technology, and software.",
            category = "Technology",
            author = "Marco Arment, Casey Liss, John Siracusa"
        ),
        DefaultPodcastFeed(
            title = "Syntax",
            url = "https://feed.syntax.fm/rss",
            description = "Tasty web development treats for frontend and full-stack developers.",
            category = "Technology",
            author = "Wes Bos and Scott Tolinski"
        ),
        DefaultPodcastFeed(
            title = "Hanselminutes",
            url = "https://feeds.simplecast.com/gvtxUiIf",
            description = "Fresh perspectives on technology, culture, and software craft.",
            category = "Technology",
            author = "Scott Hanselman"
        ),
        DefaultPodcastFeed(
            title = "ShopTalk",
            url = "https://shoptalkshow.com/feed/podcast/",
            description = "A weekly show about building websites and the open web.",
            category = "Technology",
            author = "Dave Rupert and Chris Coyier"
        ),
        DefaultPodcastFeed(
            title = "LINUX Unplugged",
            url = "https://feeds.fireside.fm/linuxunplugged/rss",
            description = "Linux news, open source stories, and community conversation.",
            category = "Technology",
            author = "Jupiter Broadcasting"
        ),
        DefaultPodcastFeed(
            title = "TED Talks Daily",
            url = "https://feeds.feedburner.com/TEDTalks_audio",
            description = "Ideas from TED speakers delivered as short daily audio episodes.",
            category = "Ideas",
            author = "TED"
        ),
        DefaultPodcastFeed(
            title = "Planet Money",
            url = "https://feeds.npr.org/510289/podcast.xml",
            description = "Economics stories told with curiosity, clarity, and a sense of play.",
            category = "Business",
            author = "NPR"
        ),
        DefaultPodcastFeed(
            title = "TED Radio Hour",
            url = "https://feeds.npr.org/510298/podcast.xml",
            description = "Manoush Zomorodi explores big ideas through TED talks and interviews.",
            category = "Ideas",
            author = "NPR"
        ),
        DefaultPodcastFeed(
            title = "Fresh Air",
            url = "https://feeds.npr.org/381444908/podcast.xml",
            description = "Long-form conversations with writers, actors, musicians, and thinkers.",
            category = "Arts",
            author = "NPR"
        ),
        DefaultPodcastFeed(
            title = "Wait Wait... Don't Tell Me!",
            url = "https://feeds.npr.org/344098539/podcast.xml",
            description = "A weekly news quiz with comedians, journalists, and listeners.",
            category = "Comedy",
            author = "NPR"
        ),
        DefaultPodcastFeed(
            title = "Hidden Brain",
            url = "https://feeds.simplecast.com/kwWc0lhf",
            description = "Science and storytelling about behavior, choices, and human patterns.",
            category = "Science",
            author = "Hidden Brain Media"
        ),
        DefaultPodcastFeed(
            title = "Lex Fridman Podcast",
            url = "https://lexfridman.com/feed/podcast/",
            description = "Long-form conversations about science, technology, history, and philosophy.",
            category = "Science",
            author = "Lex Fridman"
        ),
        DefaultPodcastFeed(
            title = "Dan Carlin's Hardcore History",
            url = "https://feeds.feedburner.com/dancarlin/history",
            description = "Expansive narrative history told with intensity and depth.",
            category = "History",
            author = "Dan Carlin"
        ),
        DefaultPodcastFeed(
            title = "Cortex",
            url = "https://www.relay.fm/cortex/feed",
            description = "Workflows, productivity, creativity, and independent work.",
            category = "Technology",
            author = "Relay FM"
        ),
        DefaultPodcastFeed(
            title = "No Such Thing As A Fish",
            url = "https://audioboom.com/channels/2399216.rss",
            description = "Interesting facts and quick wit from the QI researchers.",
            category = "Comedy",
            author = "The QI Elves"
        ),
        DefaultPodcastFeed(
            title = "In Our Time",
            url = "https://podcasts.files.bbci.co.uk/b006qykl.rss",
            description = "Melvyn Bragg and guests discuss history, philosophy, science, and culture.",
            category = "History",
            author = "BBC Radio 4"
        ),
        DefaultPodcastFeed(
            title = "Global News Podcast",
            url = "https://podcasts.files.bbci.co.uk/p02nq0gn.rss",
            description = "The day's top stories from BBC World Service.",
            category = "News",
            author = "BBC World Service"
        ),
        DefaultPodcastFeed(
            title = "The Documentary Podcast",
            url = "https://podcasts.files.bbci.co.uk/p02nq0lx.rss",
            description = "Documentaries and investigations from around the world.",
            category = "News",
            author = "BBC World Service"
        )
    )
}
