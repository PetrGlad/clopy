(ns clopy.main
  (import [java.io File]))

; Suffixes of files to be copied 
(def file-types #{"jpg" "mov" "thm" "avi" "3gp" "cr2"})

(defn media-file? [file-name] 
  (let [normalized (-> file-name .getName .toLowerCase)]
    (some #(.endsWith normalized %) file-types)))

(defn split-ext [file-name] 
  (let [dot-at  (.lastIndexOf file-name ".")]
    [(.substring file-name 0 dot-at) (.substring file-name (inc dot-at))]))

(defn target-file [source-file target-root]
  (let [source-name (.getName source-file)
        file-date-format (java.text.SimpleDateFormat. "yyyy_MM_dd-HH_mm_ss_SSS")
        section-date-format (java.text.SimpleDateFormat. "yyyy_MM_dd")
        timestamp (.lastModified source-file)
        [source-base-name source-ext]  (split-ext source-name)]
    (File. target-root (str 
                         (.format section-date-format timestamp) 
                         "/"
                         (.format file-date-format timestamp)
                         "-" source-base-name
                         "." source-ext))))

(defn main [src target]
  (println "Source" (File. src))
  (println "Target" (File. target))
  (let [source-files (filter media-file? (file-seq (File. src)))
        target-files (group-by #(target-file % target) source-files)]
    (println "Target files")
    (dorun (for [x target-files] (println x)))
    ))

; (apply main *command-line-args*)