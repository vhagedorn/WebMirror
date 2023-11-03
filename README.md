# WebMirror

A simple website archiver written in Java.

# Usage

## Downloading a site

This will take a LONG time, and create a LOT of files. Make sure the destination folder is empty! (it will create a new directory if necessary)

Also, **please note that this will only work for relatively simple sites**.
Sites that make heavy use of JavaScript (e.g. Instagram) won't archive properly!
```bash
java -jar WebMirror.jar "Destination folder" "Link to archive"
```

This will spit out a TON of messages. Don't worry! Many sites have invalid links.
Also, it will show the current status with each new link 
(sorry, idk how to use fancy floating lines in the console).

## Resuming a stopped archive

When you restart, it will re-scrape the entire site but **using files already downloaded**,
meaning that it will be _very fast_ (minutes) compared to the first run (hours/days).

On my M1 Max, I was able to archive a 38GB site in a few days,
however re-scraping all 374,564 links only takes about 20 minutes.

## Browsing the mirrored site

This archiver intends to create an exact copy of the site, as a browser sees it. 

### Quickstart
- run your webserver of choice, e.g. Python:
```bash
python3 -m http.server 8000 # (hint: try `python` if `python3` fails)
```
- then, navigate to:
> [http://localhost:8000](http://localhost:8000)

### Additional info
If you look in the destination folder, you will notice a bunch of domains outside of your target site.
These are external assets that it tries to download. Currently there is no include these while browsing
the local site (your browser still download these assets remotely), however I intend to create a Python
webserver that servers these kinds of archives properly (soon™️). Meanwhile, you can browse the mirrored
site with the default `http.server` module.


Note that it guesses file types based on extenion in the URL, so for example Wikipeia `File:name.jpg`
links will get resolved as images and downloaded, even though they are a webpage. The only solution
I can think of is guessing the file type from a stream, but that would take up bandwidth and has
diminishing returns, so I've left the behaviour as is.

# Building

```bash
git clone https://github.com/vhagedorn/WebMirror
cd WebMirror
./gradlew build
java -jar target/WebMirror.jar "dst" "url"
```

# Words

## Backstory

The concept here is very simple: recursively download an entire website by scraping
the links from each page. To my utter disbeleif, nothing like this exists (AFAIK).
Wayback machine backups don't get downloaded properly when files are SHIFT-JIS
... and people recommending `wget --mirror` are actual psychopaths. Thus,
I made this project.

## The Process

Initially, I just hacked together some Selenium code in an afternoon, which basically just
cached everything Chrome received. However, this approach had many synchronization challenges
to overcome and eventually failed with inexplicable errors. Plus, it was really slow. 
The way I'm doing it now is much faster, and involves downloading each file to the disk 
then using Jsoup to scrape every document for links using a [list of every URL attribute](src/main/java/me/vadim/archive/URIElement.java).


The sites I tend to archive sometimes have a lot of broken links, so I tried my best to 
discard 404s and generally just swallow any errors while printing a cool backtrace to see
exactly where the invalid link came from. Note that this spits out a LOT of log messages, 
and they're all saved to the `./logs` folder, so it's possible to comb for errors later.


Maybe in the future I'll revisit Chrome with fake scrolling and JS support and better caching,
but I don't think it'll happen anytime soon as I don't foresee myself archiving any sites like that.
