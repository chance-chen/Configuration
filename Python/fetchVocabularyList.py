#!/usr/bin/env python
#depends on beautifulsoup library
#sudo pip install beautifulsoup
try: 
    from BeautifulSoup import BeautifulSoup
except ImportError:
    from bs4 import BeautifulSoup
import urllib2
import sys
import requests
import os

def fetchAudio(word):
    url = "http://ssl.gstatic.com/dictionary/static/sounds/de/0/" + word + ".mp3 -o output"
    cmd = "wget " + url
    res = os.popen(cmd).readlines()

def fetchWord(word):
    url = "https://cdict.net/q/" + word
    page = urllib2.urlopen(url)
    soup = BeautifulSoup(page, "lxml")

    try:
        word = soup.body.find('span', attrs={'class' : 'word'}).text
        trans = soup.body.find('span', attrs={'class' : 'trans'}).text
        #definition = soup.bodyx.find('span', attrs={'class' : 'source'}).next_element.next_element.next_element.next_element.next_element.next_element
        definition = soup.find(attrs={"name":"description"})
        definition = str(definition).split("\"")
        #print definition[1]
        return (trans.strip(), definition[1].strip())
    except:
        return ("None", "None")

def soup_maker(url):
    soup = BeautifulSoup(requests.get(url).content)
    return soup

def fetchList(url):
    cmd = "wget " + url + " -O wgetfile -o output"
    res = os.popen(cmd).readlines()
    #page = urllib2.urlopen(url)
    #print(page)
    #soup = BeautifulSoup(page, "lxml")
    soup = BeautifulSoup(open("wgetfile"), "lxml")

    x = soup.body.findAll('li', attrs={'class' : 'entry learnable'})


    for item in x:
        word = item.find('a', attrs={'class' : 'word dynamictext'}).text
        definition = item.find('div', attrs={'class' : 'definition'}).text
        example = item.find('div', attrs={'class' : 'example'}).text
        (trans, def_tw) = fetchWord(word)
        #fetchAudio(word)
        print (word)
        print (trans)
        print "[sound:" + word + ".mp3]"
        print (def_tw)
        print(definition)
        #first slit into lines then join lines together: removing new lines
        print "".join(example.splitlines())

def main(argv):
    url = ""
    if not argv:
        #url = "http://www.vocabulary.com/lists/1411591"
        url = "http://www.vocabulary.com/lists/144636"
        #url = "https://cdict.net/?q=affirmation"
        #url = "https://cdict.net/q/affirmation"
    #req = requests.get(url, verify=False)
    fetchList(url)
    #print str(soup_maker(url))[:1000]
    #(trans, definition) = fetchWord("incredulously")
    #print trans
    #print definition

if __name__ == "__main__":
    main(sys.argv[1:])
