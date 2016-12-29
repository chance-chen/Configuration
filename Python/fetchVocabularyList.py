#!/usr/bin/env python
#depends on beautifulsoup library
#sudo pip install beautifulsoup
try: 
    from BeautifulSoup import BeautifulSoup
except ImportError:
    from bs4 import BeautifulSoup
import urllib2

page = urllib2.urlopen('https://www.vocabulary.com/lists/440306')
soup = BeautifulSoup(page, "lxml")

x = soup.body.findAll('li', attrs={'class' : 'entry learnable'})

for item in x:
    word = item.find('a', attrs={'class' : 'word dynamictext'}).text
    definition = item.find('div', attrs={'class' : 'definition'}).text
    example = item.find('div', attrs={'class' : 'example'}).text
    print word
    print definition
    #first slit into lines then join lines together: removing new lines
    print "".join(example.splitlines())
