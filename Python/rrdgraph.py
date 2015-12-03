#!/usr/bin/env python
# use rrdtool, python-rrdtool and convert
# The convert program is a member of the ImageMagick suite of tools
# sudo apt-get install rrdtool
# sudo apt-get install python-rrdtool
# sudo apt-get install ImageMagick
#
#import sys
#sys.path.append('/usr/lib/python2.7/dist-packages/')
import rrdtool
import string
from datetime import datetime
#from time import mktime
import re
import os
import sys

#set environment variable to have the correct time zone
os.environ["TZ"] = "UTC+0"

#patterns list contains variable of tuples
#each tuple contains the keyword pattern, legend of the graph and color pair
#the pattern is used to match keywords in the log, legend is used in the graph
# and color has the RRGGBB format
patterns = [
           (re.compile(r"Security Manager: btm_sec_connected in state"), "P1", "#00FF00"),
           (re.compile(r"btm_sec_disconnected\(\) sec_req\:"), "P2", "#FF0000")
           #(re.compile(r"com.asus.wellness"), "P1", "#00FF00"),
           #(re.compile(r"bt_host_wake"), "P2", "#FF0000")
           ]

graph_fname = "test.gif" #filename of the graph generated
log_fname = "logcat.txt" #filename of the input log

#what year is this year? used for prepend before date string
this_year = "2015"
step = 0 #how many seconds per step, 0: auto adjustment
stime = 0 #start time
etime = 0 #end time
db_fname = "test.rrd" #filename of the database, used internally to store data and output graph

def unix_time(dt):
    epoch = datetime.utcfromtimestamp(0)
    delta = dt - epoch
    return int(delta.total_seconds())

#compute how many rows is needed in RRA
#input stime etime and step
#output rounded stime etime that will cover the period and rows
def compute_rows(stime, etime, step):
    n = stime / step
    stime = n * step
    n = etime / step
    if etime % step == 0:
        etime = n * step
    else:
        etime = (n + 1) * step
    return (stime, etime, (etime - stime) / step)

def create(name, stime, step, rows):
    args = []
    args.append(name)
    args.append("-b " + str(stime))
    args.append("-s " + str(step))
    #variable part
    for (pattern, legend, color) in patterns:
        args.append("DS:" + legend + ":GAUGE:" + str(step) + ":U:U")
        args.append("RRA:LAST:0.99:1:" + str(rows))
    rrdtool.create(args)

#cannot update twice in the same second
#update database of the previous second in a second later
#update.last_update = 0
def update(epoch, values):
    if int(epoch) > int(update.pending_update): #new interval

        arg = str(update.pending_update)
        for value in update.pending_values:
             arg += ":" + str(value)
        rrdtool.update(db_fname, arg)
        update.pending_update = epoch
        for i in range(len(values)):
            update.pending_values[i] = values[i]
    else: #old inverval
        for i in range(len(values)):
            if int(values[i]) > int(update.pending_values[i]):
                update.pending_values[i] = values[i]

if __name__ == '__main__':

    try:
        log_fname = sys.argv[1]
        print "use file name = " + log_fname
    except:
        print "use default file name " + log_fname

    try:
        f = open(log_fname)
    except:
        print "failed to open " + log_fname
        exit(0)

    #first pass, find out start time and end time in the logs
    for line in f.readlines():
        try:
            lines = string.splitfields(line, " ")
            datetimestr = this_year + "-" + lines[0] + " " + lines[1]
            date_object = datetime.strptime(datetimestr, '%Y-%m-%d %H:%M:%S.%f')
            #etime = mktime(date_object.timetuple())
            etime = unix_time(date_object)
            if stime == 0:
                stime = etime
        except:
            print line

    #assume 1200 pixels, if step too short it may not be shown on the graph
    step = (etime - stime) / 1200
    if step < 3:
        step = 3

    #find out how many rows are needed
    (stime, etime, rows) = compute_rows(int(stime), int(etime), step)
    #print "stime = " + str(stime) + " etime = " + str(etime) + " step = " + str(step)
    #create the database to store data
    create(db_fname, stime, step, rows)

    #seek to the first line
    f.seek(0)

    num_patterns = len(patterns)
    #start time of DB is stime, start to update one second later
    #print "stime = " + str(stime)
    #print "etime = " + str(etime)
    update.pending_update = stime + 1
    update.pending_values = []
    values = []
    print "duration = " + str(etime - stime) + " seconds step = " + str(step)
    for i in range(num_patterns):
        update.pending_values.append(0)
        values.append(0)
        (pattern, legend, color) = patterns[i]
        print "Pattern: " + pattern.pattern 
        print " Legend: " + legend
        print "  Color: " + color

    #(pattern, legend, color) = patterns[0]
    tick = stime - 1

    for line in f.readlines():
        try:
            lines = string.splitfields(line, " ")
            datetimestr = this_year + "-" + lines[0] + " " + lines[1]
            date_object = datetime.strptime(datetimestr, '%Y-%m-%d %H:%M:%S.%f')
            epoch = unix_time(date_object)
            #update '0' if not present in the log
            while tick < epoch:
               #print "tick = " + str(tick)
               #print "epoch = " + str(epoch)
               for j in range(num_patterns):
                   values[j] = "0"
               update(str(tick), values)
               tick += 1


            for i in range(num_patterns):
                (pattern, legend, color) = patterns[i]
                if pattern.search(line):
                    values[i] = "1";
                    #print values
                    #print "match " + str(epoch) + " i = " + str(i)
                    #print line
                else:
                    values[i] = "0";
            update(str(epoch), values)
        except:
            print line

    #trigger the last pending update
    update(str(etime + 1), values)
    #args = ["test.gif", "-s " + str(stime), "-e " + str(etime), "DEF:P1=test.rrd:P1:LAST", "CDEF:P11=P1,0,GT", "AREA:P11#FF0000:P11"]

    args = []
    args.append(graph_fname)
    args.append("-s " + str(stime))
    args.append("-e " + str(etime))
    #width is 1200 pixels
    args.append("-w " + "1200")
    #args.append("-A")
    #args.append("-e " + str(stime+1800))
    for i in range(num_patterns):
        (pattern, legend, color) = patterns[i]
        args.append("DEF:" + str(legend) + "=" + db_fname + ":" + str(legend) + ":LAST")
        args.append("CDEF:" + str(legend) + "1=" + str(legend) + ",0,GT")
        args.append("AREA:" + str(legend)  + "1" + str(color) + ":" + str(legend))
    rrdtool.graph(args)
    #print "Graph generated and output to " + graph_fname

    #output individual graphs
    for i in range(num_patterns):    
        (pattern, legend, color) = patterns[i]
        args = []
        args.append(str(legend) + "_" + graph_fname)
        args.append("-s " + str(stime))
        args.append("-e " + str(etime))
        #width is 1200 pixels
        args.append("-w " + "1200")
        #args.append("-A")
        #args.append("-e " + str(stime+1800))
        args.append("DEF:" + str(legend) + "=" + db_fname + ":" + str(legend) + ":LAST")
        args.append("CDEF:" + str(legend) + "1=" + str(legend) + ",0,GT")
        args.append("AREA:" + str(legend)  + "1" + str(color) + ":" + str(legend))
        rrdtool.graph(args)
        #print "Graph generated and output to " + str(legend) + "_" + graph_fname

    #convert all images
    cmd = "convert " + graph_fname
    for i in range(num_patterns):
        (pattern, legend, color) = patterns[i]
        cmd += " " + str(legend) + "_" + graph_fname
    cmd += " -append " + "A_" + graph_fname
    os.popen(cmd)
    print "Graph combined generaged to " + "A_" + graph_fname
