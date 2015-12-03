#!/usr/bin/env python
import os
import string
import time
import re
import random

#how many times
numtest = 3000
#sleep base time after reboot(sec) actual sleep idletime + [0~480]
idletime = 20

def kill_bluetooth():
    cmd = "adb shell ps | grep com.android.bluetooth"
    #print cmd
    res = os.popen(cmd).readlines()
    for line in res:
        pid = line.split( )[1]
        cmd = "adb shell kill " + pid
        print cmd
        res = os.popen(cmd).readlines()
    return 0

for i in range(numtest):
    print "round:" + str(i)
    start_time = time.time()
    kill_bluetooth()
    #time.sleep(20)
    time.sleep(20)
    end_time = time.time()
    #print "round:" + str(i) + " " + str(end_time - start_time) + " sec"
