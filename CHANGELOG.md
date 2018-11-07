## Changelog 

### 2.7.0 (pending)

#### rc6

* dumped live support for embedded modules (to simplify the code)
* legacy option is optional now
* secrets path separator is a comma now


#### rc5 (2018-11-06)

* fixed md5 caching problem
* launch with --illegal-access=deny when running on Java 9+


#### rc4

* fixed permissions for svn cache files

#### rc3

* work-around for application-parent bug: automatically prepend missing scm:svn: to prod urls (rc2 did this for devel urls only)

#### rc2

* fix module name for scm commands

#### rc1

* added support for bitbucket modules: derive you pom from at least frontend-parent 3.4.0 or application-parent-3.10.0;
  note that loading resources from the server does not see local changes that have not been pushed yet;
  * caution: Bitbucket passwords do not equal intranet passwords; it's usually not defined because ssh is used for cloning a repository,
    you can set it with

* DevelopmentFilter
  * use etags only (based on Resource.getContentId), last-modified header is gone

* cli
  * dumped `-lastconfig` switch, it was not used by puc, and you can now reconfigure `network` in the host properties
  * generalized `svn` command to `scm`; the first argument is now the scm name defined in the host properties
  * simplified command line by aligning the arguments passed to the various publishing commands:
    * 'war': it's just `war cluster docroot index` now
    * 'scm': it's just `scm cluster docroot index` now
    * 'file': mandatory arguments are now `archive cluster docroot index`

* improved locking of cluster hosts
  * remove connection locks on ctrl-c (via shutdown hook)
  * lock files now contain user, machine and command being executed

* caching:
  * dumped cache lock - use atomic reads and writes instead
  * introduced md5 cache maintained by Distributor for all modules
  * changed svn entry cache: dumped md5, size and last modified fields; svn caches entries now reside under <cachdir>/svn

* module configuration cleanup
  * svn properties generalized to scm properties (old svn properties still work, but both application- and frontend-parents should be
    updated soon; they have to be updated to get bitbucket support)
    * svn prefix changed to scm
    * dumped `scm.foo.relative`, it was always empty
    * added `scm.foo.path` to configure the path formerly appended to the url or devel url (typically `src/main/resources`)
    * replaced `revision` by `tag`
    * dumped pustefix.* properties; thus, it's no longer possible to configure embedded resources;
      report an error if legacy descriptors contain a matchable filter
    * added a `legacy` property to configure old modules; use `lavender scan-legacy` to search a project for legacy modules;
      lavender repos an error if a new descriptor does not define this property
  * pominfo.properties, lavender.properties and resource index are now required for lavender modules

* admin configuration cleanup
  * system properties -> host properties
    * changed terminology: system properties are now called host properties (to better distinguish them from Java's system properties)
    * renamed `lavender.properties` to `lavender/host.properties`; also renamed the corresponding system property `lavender.properties` to `lavender.hostproperties`
      and the environment variable `LAVENDER_PROPERTIES` to `LAVENDER_HOSTPROPERTIES`
    * added `location` property to configure the location of the network.xml
    * instead of a single svn url, you can now configure a map of urls (including git urls)
    * configurable secrets path: `secrets` defines a comma-separated (CAUTION: not colon, because that's used in fault file names) path where to search for secrets files; wildcard allowed
  * renamed net.xml to network.xml
  * in network.xml
    * renamed docroot type to docroot name
    * renamed docroot docroot to docroot documents
    * merge aliases into docroot

* added java 11 support
  * fixed hardcoded references to com.sun.zipfs
  * added explicit javax.xml.bind dependencies
  * launcher adds the respective java 9+ options when $JAVA_MODULES is defined

* dependency updates:
  * sushi 3.1.6 to 3.1.7
  * svnkit 1.8.5 to 1.9.3
  * slf4j 1.7.5 to 1.7.25



### 2.6.2 (2018-03-12)

* fixed file creation problem introduced in 2.6.1: infinite loop if _tmp_1 exists (thanks to Marcus T)


### 2.6.1 (2017-09-01)

* fixed permission problem on svn cache: create files with permissions based on umask 
  (2.6.0 created files with rw- --- --- permissions)


### 2.6.0 (2017-08-29)

* added fsck -repair-all-idx option 
* CssProcessor url rewrite fix: don't try to rewrite data urls (thanks to Julian B)
* Updated Sushi 2.8.x to 3.1.x and Inline 1.1.1 and Metadata 1.0.0 (thanks to Marcus T)
* Updated Jsch 0.1.51 to 0.1.54


### 2.5.5 (2016-12-21)
q
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
