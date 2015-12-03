;;update 2009-12-26
(global-set-key "\C-l" 'goto-line) ;; goto-line
(global-set-key "\C-u" 'scroll-down) ;; goto previous page
(global-set-key "\C-b" 'backward-page) ;; goto begining of the file
(global-set-key "\C-n" 'forward-page) ;; goto end of the file
(global-set-key "\C-w" 'backward-kill-word) ;; backward kill word
;;(transient-mark-mode 1) ; highlight text selection
;;(delete-selection-mode 1) ; delete seleted text when typing
(show-paren-mode 1) ; turn on paren match highlighting
;; start current line highlight
;;(hl-line-mode 1)
(global-hl-line-mode 1)
;;(set-face-background 'hl-line "MediumAquamarine")  ;; Emacs 22 Only
;;to use more colors set 'export TERM="xterm-256color"' in .bashrc
;;(set-face-background 'hl-line "#222")  ;; Emacs 22 Only
(set-face-background 'hl-line "#222")  ;; Emacs 22 Only
;(set-face-background 'hl-line "gray45")  ;; Emacs 22 Only
; (set-face-background 'highlight "#330")  ;; Emacs 21 Only
;; end current line highlight
;;disable vc-git mode, since it loads so slow
(setq vc-handled-backends (quote ()))
;;Highlights pairs of parenthesis
;(electric-pair-mode 1)
(defun linux-c-mode ()
"C mode with adjusted defaults for use with the Linux kernel."
(interactive)
(c-mode)
(c-set-style "K&R")
(setq tab-width 4)
(setq indent-tabs-mode t)
;;(setq c-basic-offset 8))
(setq c-basic-offset 4))
(setq c-default-style "linux-c")
(setq-default indent-tabs-mode nil)
;; start evernot;; end evernote
(require 'ido)
(ido-mode t)
