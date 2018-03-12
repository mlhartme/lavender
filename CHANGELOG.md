## Changelog 

### 2.6.2 (2018-03-12)

* fixed file creation problem introducted in 2.6.1: infinite loop if _tmp_1 exists (thanks to Marcus T)


### 2.6.1 (2017-09-01)

* fixed permission problem on svn cache: create files with permissions based on umask 
  (2.6.0 created files with rw- --- --- permissions)


### 2.6.0 (2017-08-29)

* added fsck -repair-all-idx option 
* CssProcessor url rewrite fix: don't try to rewrite data urls (thanks to Julian B)
* Updated Sushi 2.8.x to 3.1.x and Inline 1.1.1 and Metadata 1.0.0 (thanks to Marcus T)
* Updated Jsch 0.1.51 to 0.1.54


### 2.5.5 (2016-12-21)

* Added `remove-entry` command


### 2.5.4 (2016-10-24)

* Fix CSS image urls with quotes, whitespaces and escaped chars


### 2.5.3 (2016-10-20)

* production/development mixed mode can be triggered via system property 'lavender.allowProdDevMixMode'


### 2.5.2 (2016-09-30)

* Also use production and development filter in test environment.


### 2.5.1 (2016-09-12)

* HtmlProcessor refactoring for standalone usage.
* Fix java debian dependency, see https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=803152.


### 2.5.0 (2016-03-22)

* Make Lavender filter reloadable
* Refactored Lavender filter by ProductionFilter and DevelopmentFilter


### 2.4.2 (2016-02-11)

* Update sushi 2.8.18 to 2.8.19 to get support for configurable ssh ports.


### 2.4.1 (2016-01-26)

* Fixed tailing space problem when loading devel svn caches.


### 2.4.0 (2016-01-16)

* Dumped /opt/ui from binary and properties path. Adjusted properties search path accordingly.
* Print servlet filter startup exceptions to stderr because logging might not be configured yet.
* Properly unlock svn cache when servlet filter startup fails with an exception.
* Properly document and check the first file command argument. It's now called archive.
* Improved search order to locate lavender.properties: It's now what you'll normally expect:
  1) "lavender.properties" system property
  2) "LAVENDER_PROPERTIES" environment variable
  3) classpath
  4) ~/.lavender.properties
  5) /etc/lavender.properties
 Thus, there's a new system property that's searched first. And the classpath is now searched before the home directory.
* Support configurable uri for hosts. This replaces the option to specify a path.


### 2.3.0 (2015-08-04)

* Added revision property to svn properties to pin a module to one revision.
* lavender.properties: Added optional cache property to configure svn cache directory. Default is ~/.cache/lavender.
  The cache directory may be shared, it's properly locked and cache files are deleted before writing to avoid permission
  problems.
* Read svn resource with the scanned revision, not with latest revision.
* The war command takes on war argument now, and this war file is modified. 
  This is an incompatible change, and this is the reason for the minor version bump.
* Speedup Svn modules: fixed path problem that prevented some caching;
  read without tmp files; get rid of checkPath call; cache scan results; 
  append to war using TrueZip. 


(See https://github.com/mlhartme/lavender/blob/lavender-2.2.6/src/changes/changes.xml for older changes)
