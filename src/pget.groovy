/*
 How to get and use this script
   0. Get Groovy from http://groovy.codehaus.org/
1. Download this script as pget.groovy (click the 'raw' link on this github page.)  --------------------^^^
    2. Create a directory to save the podcast in (for example, mkdir /tmp/podcasts )
3. Edit pget.groovy and change the sites to be url's to podcasts you like
   4. Run like this, "groovy pget.groovy <downloadLocation>" (ie. groovy pget.groovy /tmp/podcasts)
 Enjoy.

I use this script to download new mp3's into a directory.   I then copy the files onto my mp3 player and
    listen to them during my commute.  When I'm need a recharge of new mp3s, I re-run the script to get more podcasts.

This script;
- numbers the files uniquely (so there aren't any naming collisions)
- preserves the download order so I hear my most important podcasts first.
    - keeps track of what is downloaded (so you don't get the same podcasts over and over.)
- only downloads a max of 3 podcasts from each source (so when first using this script you don't get 100 podcasts)
- You can stop the script and edit and re-run it (tweaker friendly). Rerunning is safe because it doesn't update the history until the end and it skips already downloaded files.

*/
// groovy 1.6beta2 has a bug. uncomment this line to use it.
// def args = [ "/tmp/podcasts" ]
if (args.size() != 1 ){
    println "usage: pget downloadDirectory"
    System.exit(1)
}
def downloadLocation = args[0]
if (  !new File(downloadLocation).isDirectory() ){
    println "Error: The argument (${downloadLocation}) is not a directory."
    System.exit(-1);
}
downloadLocation += File.separator + "%04d-%s"

def downloadHistory = []
def downloadHistoryFile = new File(System.properties['user.home']+File.separator+'.pgetDownloadHistory');
if ( downloadHistoryFile.exists() ){
    downloadHistory = evaluate(downloadHistoryFile.text)
} else {
    println "Warning: No download history found, creating a new one."
}

println "running, cached: " + downloadHistory.size()

sites = [
    "http://podcasts.cstv.com/feeds/fantasyplaybook.xml",
    "http://podcasts.cstv.com/feeds/fantasyfootball.xml",
    "http://feeds.feedburner.com/linuxoutlaws",
    "http://www.thelinuxlink.net/tllts/tllts.rss",
    "http://feeds.feedburner.com/dailybreakfast",
    "http://blogs.sun.com/theplanetarium/feed/entries/atom",
    "http://feeds.wnyc.org/radiolab?utm_source=rss&utm_medium=hp&utm_campaign=radiolab",
    "http://feeds.feedburner.com/catholicinsider",
    "http://feeds.feedburner.com/TheSwordAndLaser",
    "http://feeds2.feedburner.com/JupiterBroadcasting",
    "http://feeds2.feedburner.com/ChariotTechCast",
    "http://feeds2.feedburner.com/WebdevradioPodcastHome",
    "http://feeds2.feedburner.com/PhandroidPodcast",
    "http://feeds2.feedburner.com/ThisAintYourDadsJava",
    "http://feedproxy.google.com/androidguyscom",
    "http://hansamann.podspot.de/rss",
    "http://www.pbs.org/cringely/pulpit/rss/podcast.rss.xml",
    "http://feeds.feedburner.com/javaposse",
    "http://feeds.feedburner.com/rubyonrailspodcast",
    "http://blog.stackoverflow.com/index.php?feed=podcast",
    "http://www.nofluffjuststuff.com/s/podcast/itunes.xml",
    "http://feeds.feedburner.com/Softballpodcasts",
    "http://media.ajaxian.com/",
    "http://agiletoolkit.libsyn.com/rss",
    "http://feeds.feedburner.com/geeklunch",
    "http://feeds.feedburner.com/rubyshow",
    "http://feeds.feedburner.com/utilitybelt",
    "http://www.discovery.com/radio/xml/sciencechannel.xml",
    "http://feeds.feedburner.com/gigavox/channel/itconversations",
    "http://www.scienceandsociety.net/podcasts/index.xml",
    "http://leoville.tv/podcasts/floss.xml",
    "http://aboutgroovy.com/podcast/rss",
    "http://leoville.tv/podcasts/twig.xml",
    "http://rssnewsapps.ziffdavis.com/audioblogs/crankygeeks/cg.audio.xml",
    "http://feeds.feedburner.com/DrunkAndRetiredAudio?format=xml",
    "http://feeds.feedburner.com/elegantcodecast",
    "http://www.extra-points.com/rss",
    "http://www.sportsline.com/xml/podcasts/fantasyfootball.rss",
    "http://feathercast.org/?feed=rss2",
    "http://leoville.tv/podcasts/fib.xml",
    "http://feeds.feedburner.com/GoogleDeveloperPodcast",
    "http://feeds.feedburner.com/HdtvPodcast",
    "http://www.ibm.com/developerworks/podcast/channel-dwall.rss",
    "http://www.cincomsmalltalk.com/rssBlog/blog_podcast.xml",
    "http://www.javaworld.com/podcasts/jtech/index.xml",
    "http://www.muskiefirst.com/podcast",
    "http://feeds.feedburner.com/OpenWebPodcast",
    "http://parleys.libsyn.com/rss",
    "http://pragprog.com/podcasts/feed.rss",
    "http://leoville.tv/podcasts/sn.xml",
    "http://www.softwareas.com/podcast/rss2",
    "http://www.se-radio.net/rss",
    "http://feeds.feedburner.com/TechLuminaries",
    "http://www.mevio.com/feeds/tech5.xml",
    "http://johannarothman.libsyn.com/rss",
    "http://thestacktrace.libsyn.com/rss/mp3",
    "http://leoville.tv/podcasts/twit.xml",
    "http://feeds2.feedburner.com/tdicasts",
    "http://anteupmagazine.com/podcast/podcast.xml"
]
enclosures = [:]

histmap = [:]
downloadHistory.each {urlName, fileName ->
        histmap[urlName] = fileName
}

sites.each {site ->
        //site = sites[0]
    println "site: ${site}"
    try
    {
    xml = new groovy.util.XmlSlurper().parse(site)

    int max = 2;

    def xmlenclosures = xml.depthFirst().findAll { it.name().equals("enclosure") }
    println "   has ${xmlenclosures.size()} enclosures"
    xmlenclosures.each {enclosure ->
            //println "enclosure " + enclosure.@url + " or " + enclosure.@url.toString()

        url = enclosure.@url.toString()
        filename = url.toString().substring(url.toString().lastIndexOf('/') + 1);
        //println "$filename  of  $url"

        if (max-- > 0) {
            if (histmap[filename]) {
                println "already have $filename"
                // prevend downloading past 'areadly have'
                max = 0;
            } else {
                println "Will get: " + filename + " " + url
                enclosures[url] = filename
            }

        }
    }
}catch(Exception e)
{
    println "Error with ${site}"
    println e
}
}

println "\nstarting downloads... will get " + enclosures.size() + " files"

errors = 0;

enclosures.each {url, filename ->
        println "downloading: " + filename + " " + url
    ondiskname = String.format(downloadLocation, downloadHistory.size(), filename)

    file = new File(ondiskname)
    file.parentFile.mkdir()

    if (file.exists()) {
        println "**** ALREADY DOWNLOADED: ${url}"
    } else {
        def fileOut = new FileOutputStream(file)
        def out = new BufferedOutputStream(fileOut)
        out << new URL(url).openStream()
        out.close()

        [ "/usr/bin/id3v2", "-t", '_' + file.name, "-A", "PODCAST", "-g", "12", "-T", "${downloadHistory.size()}", ondiskname ].execute()
    }
    println "ok have downloadHistory[$filename]=$ondiskname"

    odnf = new File(ondiskname);
    downloadHistory << [filename, odnf.name]
}

// saved new items
downloadHistoryFile.withPrintWriter {pw ->
        pw.println("def history = [ ")
    downloadHistory.each {
        pw.println " [ '${it[0]}', '${it[1]}' ],"
    }
    pw.println("]")
}

println "all done, cache:" + downloadHistory.size() + ", added:" + enclosures.size() + ", errors:" + errors
