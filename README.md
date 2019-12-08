# ctrail
Color Trail: A tool that expands on the functionality of `tail -f` by produces customizable, colored output. It can read from files or standard in, and can be customizable per directory. 


## Configuration / Customization
### Per execution configuration
The order of searching for a valid config file is:

```
	-> Java System Property: -DCTRAIL_CFG=XXX
		-> current working directory (./ctrail.xml)
			-> system config file (/etc/ctrail.xml)
```

In other words, it looks in the current working directory for any localization files; if not found, it looks in `/etc/ctrail.xml`. At least one of the config entries must be present, or the system will not run.


## Files
### Installation locations
* bash script: `ctr`, usually located in `/usr/local/bin/`
* library file: 
    * usually located in: `/usr/local/lib/ctrail-XXX.jar`
    * linked (ln -s) to `ctrail.jar`

* config file: `/etc/ctrail.xml` 
    * any local configs you want