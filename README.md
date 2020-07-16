# data-organizer
Organize your multi file excels in one integrated file, ready to analyze

- create a new folder with any name
- build or download your jar an place it in your new folder
- add folders named "config", "raw", "adjusted" and "map" under your new folder
- drag all your excels in folder named raw
- for the moment I support just proper .xls files. if your files are not xls files, you can easily convert them with "Save As" => "Excel - 97 / 2003"
- copy one of the raw excel files in folder named "raw"
- mark the cells which contains meaningful values on all the files:
- click the meaningful cell and write a variable name instead of its original value
- clear all the other cells: it will be enough to delete the values inside the cells, the format and styles are not important
- open CMD or terminal in your folder, where the jar is placed
- command "java -jar data-organizer-1.0.0.jar"
- you will find integrated and adjusted file under the folder named "adjusted"
