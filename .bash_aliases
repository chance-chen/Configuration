#update 2009-12-26
alias c=clear
alias c.='cd ..'
alias c..='cd ../..'
alias l='ls -F'
alias netstat='netstat -tlunp'
alias emacs='emacs -nw'
alias e='emacs -nw'
alias em='emacs -nw'
alias grep="grep -I --color=always"
#working together with grep --color to make colorful grep with less
alias less="less -r"
alias netstat='netstat -tlunp'
alias go=godir
alias adb="/usr/bin/adb"
alias du="du --max-depth=1 -B M"
alias hcipull="adb pull /sdcard/btsnoop_hci.log;hcidump -r btsnoop_hci.log -Xt > hcidump.txt;emacs -nw hcidump.txt"