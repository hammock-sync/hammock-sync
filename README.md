# Hammock Sync Android
[![JitPack core](https://jitpack.io/v/hammock-sync/hammock-sync.svg)](https://jitpack.io/#hammock-sync/hammock-sync)

| Artifact | Links                                                                                                                                                                                                                                   |
| ---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Core | [![JitPack core](https://img.shields.io/jitpack/v/hammock-sync/hammock-sync?label=Jitpack%20Core)](https://jitpack.io/com/github/hammock-sync/hammock-sync/datastore-core/1.0.0-SNAPSHOT/)                                              |
| Android | [![JitPack android](https://img.shields.io/jitpack/v/hammock-sync/hammock-sync?label=Jitpack%20Android)](https://jitpack.io/com/github/hammock-sync/hammock-sync/datastore-android/1.0.0-SNAPSHOT/")                                    |
| Android (encryption) | [![JitPack Android Encryption](https://img.shields.io/jitpack/v/hammock-sync/hammock-sync?label=Jitpack%20Android%20Encryption)](https://jitpack.io/com/github/hammock-sync/hammock-sync/datastore-android-encryption/1.0.0-SNAPSHOT/") | 
| Java SE | [![Jitpack javase](https://img.shields.io/jitpack/v/hammock-sync/hammock-sync?label=Jitpack%20JavaSE)](https://jitpack.io/com/github/hammock-sync/hammock-sync/datastore-javase/1.0.0-SNAPSHOT/")                                       |


**Applications use Hammock Sync to store, index and query local JSON data on a
device and to synchronise data between many devices. Synchronisation is under
the control of the application, rather than being controlled by the underlying
system. Conflicts are also easy to manage and resolve, either on the local
device or in the remote database.**

Hammock Sync is an [Apache CouchDB&trade;][acdb]
replication-protocol-compatible datastore for
devices that don't want or need to run a full CouchDB instance. It was originally built
by [Cloudant](https://cloudant.com), building on the work of many others and now maintained and modernized
by the Hammock Sync project.  

Hammock Sync project is a derivative work form Cloudant Sync for Android which has been abandoned by
its original authors. Hammock Sync is a new effort for keeping this CouchDB Sync Client for Android
and Java alive.

Hammock Sync project is distributed under [Apache 2.0 licence][ap2] in the same form that the original
project and keeps the original copyright on those elements retrieved from the original project.

[ap2]: https://github.com/cloudant/sync-android/blob/master/LICENSE
[acdb]: http://couchdb.apache.org/

The API is quite different from CouchDB's; we retain the
[MVCC](http://en.wikipedia.org/wiki/Multiversion_concurrency_control) data
model but not the HTTP-centric API.

## Using in your project

The library is currently published via Jitpack and using it in your project should
be as simple as adding it as a dependency via [maven][maven] or [gradle][gradle]. 

Jitpack repository should be declared at the build.gradle file. 
```
    maven { url 'https://jitpack.io' }
```

[maven]: http://maven.apache.org/
[gradle]: http://www.gradle.org/

There are currently four artifacts for the datastore, two jar and two aar:

* `datastore-core`: jar with the main datastore classes.
* `datastore-android`: aar with Android specific classes.
* `datastore-android-encryption`: aar with Android encryption specific classes.
* `datastore-javase`: jar with Java SE specific classes.
