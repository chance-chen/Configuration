#!/usr/bin/env python
import os
import string
import time
import re
import random

#how many times
numtest = 3000
#sleep base time after reboot(sec) actual sleep idletime + [0~480]
idletime = 120

def bt_enable():
    cmd = "adb shell service call bluetooth_manager 6"
    print cmd
    res = os.popen(cmd).readlines()
    print res
    return 0

#is_enabled service call bluetooth_manager 5

def bt_disable():
    cmd = "adb shell service call bluetooth_manager 8 i32 1"
    print cmd
    res = os.popen(cmd).readlines()
    return 0

def wait_adb():
    cmd = "adb wait-for-device"
    print cmd
    res = os.popen(cmd).readlines()
    cmd = "adb root"
    print cmd
    res = os.popen(cmd).readlines()
    cmd = "adb wait-for-device"    
    print cmd
    res = os.popen(cmd).readlines()
    return 0

def adb_reboot():
    cmd = "adb reboot"
    print cmd
    res = os.popen(cmd).readlines()
    return 0

def start_apps1():
    cmd = "adb shell am start com.asus.calendar"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.asus.task"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.asus.ephoto"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.asus.contacts"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.asus.filemanager"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.asus.calculator"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.asus.deskclock"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.asus.music"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.asus.email"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start -n com.ea.games.r3_row/com.firemint.realracing3.MainActivity"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)


def start_apps2():
    cmd = "adb shell am start com.android.chrome"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.google.android.gm"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.google.android.music"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.google.android.gms"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.google.android.youtube"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.google.android.apps.plus"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)
    cmd = "adb shell am start com.google.android.talk"
    print cmd
    res = os.popen(cmd).readlines()
    time.sleep(3)

def copy_logcat(i):
    cmd = "adb pull /data/logs/aplog logs/aplog." + str(i)
    print cmd
    res = os.popen(cmd).readlines()

#create logs directory
if not os.path.isdir("logs"):
    os.popen("mkdir logs")

for i in range(numtest):
    print "round:" + str(i)
    start_time = time.time()
    wait_adb()
    print "sleep 30 sec"
    time.sleep(30)
    start_apps1()
    sleeptime = idletime + random.randint(0, 480)
    print "sleep " + str(sleeptime) + " sec"
    time.sleep(sleeptime)
    start_apps2()
    bt_enable()
    time.sleep(10)
    bt_disable()
    copy_logcat(i)
    time.sleep(10)
    end_time = time.time()
    print "round:" + str(i) + " " + str(end_time - start_time) + " sec"
    adb_reboot()
