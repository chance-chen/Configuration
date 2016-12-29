#!/usr/bin/env python
# remove extra btsnoop headers
# only header at position 0 is reserved
#
import sys

DEFAULT_LOG_NAME = "btsnoop_hci.log"
BTSNOOP_HEADER = b'\x62\x74\x73\x6e\x6f\x6f\x70\x00\x00\x00\x00\x01\x00\x00\x03\xea'

def parseBtsnoopHdr(log_fname):
    f = open(log_fname, 'rb')
    file_pos = 0 #current position in the file
    hdr_index = 0 #match index in the header
    data_in = b''
    hdr_pos = [] #header starting positions

    #if next byte from file is the same as next byte from header
    while True:
        data_in = f.read(1)
        if not data_in: break
        file_pos = file_pos + 1
        if (data_in == BTSNOOP_HEADER[hdr_index]):
            hdr_index = hdr_index + 1
        else:
            hdr_index = 0
        if (hdr_index == len(BTSNOOP_HEADER)):
            print "btsnoop Header found at " + str(file_pos - len(BTSNOOP_HEADER))
            hdr_pos.append(file_pos - len(BTSNOOP_HEADER))
            hdr_index = 0

    #we append the end of the file position:
    # the writing process does not need to consider the special case of EOF
    hdr_pos.append(file_pos)

    f.close()
    if len(hdr_pos) <= 2:
        print "no duplicate btsnoop hdrs"
        if 0 in hdr_pos:
            print "header at pos 0 detected"
        else:
            print "there is no header at pos 0"
            #hdr_pos.insert(0, 0)
        return

    #we're going to remove exra headers
    out_fname = log_fname + "_rmExtraHdrs"
    print "hdr_pos " + str(hdr_pos)
    fi = open(log_fname, 'rb')
    fo = open(out_fname, 'wb')

    #first, write the header
    fo.write(BTSNOOP_HEADER)

    pre_pos = 0
    for pos in hdr_pos:
        if pos == 0: continue #skip pos 0
        #writing pre_pos + len(HDR) through pos
        print "write " + str(pre_pos + len(BTSNOOP_HEADER)) + " through " + str(pos)
        fi.seek(pre_pos + len(BTSNOOP_HEADER))
        data_in = fi.read(pos - pre_pos - len(BTSNOOP_HEADER))
        fo.write(data_in)
        
        pre_pos = pos
 
    fo.close()
    fi.close()

if __name__ == '__main__':
    try:
        log_fname = sys.argv[1]
        print "use file name = " + log_fname
    except:
        log_fname = DEFAULT_LOG_NAME
        print "use default file name " + log_fname
    parseBtsnoopHdr(log_fname)
