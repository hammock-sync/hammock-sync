# Hammock Sync Android (DRAFT - NOT YET PUBLISHED)
[![JitPack core](https://jitpack.io/v/hammock-sync/hammock-sync.svg)](https://jitpack.io/#hammock-sync/hammock-sync)

| Artifact | Links |
| ---|--- |
| Core | [![JitPack core](https://img.shields.io/jitpack/v/hammock-sync/hammock-sync?label=Jitpack%20Core)](https://jitpack.io/com/github/hammock-sync/hammock-sync/hammock-sync-datastore-core/0.0.0-SNAPSHOT/) |
| Android | [![JitPack android](https://img.shields.io/jitpack/v/hammock-sync/hammock-sync?label=Jitpack%20Android)](https://jitpack.io/com/github/hammock-sync/hammock-sync/hammock-sync-datastore-android/0.0.0-SNAPSHOT/") |
| Android (encryption) | [![maven-central-android-encryption](https://img.shields.io/jitpack/v/hammock-sync/hammock-sync?label=Jitpack%20Android%20Encryption)](https://jitpack.io/com/github/hammock-sync/hammock-sync/hammock-sync-datastore-android-encryption/0.0.0-SNAPSHOT/") | 
| Java SE | [![Jitpack javase](https://img.shields.io/jitpack/v/hammock-sync/hammock-sync?label=Jitpack%20JavaSE)](https://jitpack.io/com/github/hammock-sync/hammock-sync/hammock-sync-datastore-javase/0.0.0-SNAPSHOT/") |


**Applications use Hammock Sync to store, index and query local JSON data on a
device and to synchronise data between many devices. Synchronisation is under
the control of the application, rather than being controlled by the underlying
system. Conflicts are also easy to manage and resolve, either on the local
device or in the remote database.**

Hammock Sync is an [Apache CouchDB&trade;][acdb]
replication-protocol-compatible datastore for
devices that don't want or need to run a full CouchDB instance. It was originally built
by [Cloudant](https://cloudant.com), building on the work of many others and now maintained and modernized
by the Hammock Sync project. This work is available under the [Apache 2.0 licence][ap2].

[ap2]: https://github.com/cloudant/sync-android/blob/master/LICENSE
[acdb]: http://couchdb.apache.org/

The API is quite different from CouchDB's; we retain the
[MVCC](http://en.wikipedia.org/wiki/Multiversion_concurrency_control) data
model but not the HTTP-centric API.


## Using in your project

The library is published via Maven Central and using it in your project should
be as simple as adding it as a dependency via [maven][maven] or [gradle][gradle].

[maven]: http://maven.apache.org/
[gradle]: http://www.gradle.org/

There are currently four artifacts for the datastore, two jar and two aar:

* `hammock-sync-datastore-core`: jar with the main datastore classes.
* `hammock-sync-datastore-android`: aar with Android specific classes.
* `hammock-sync-datastore-android-encryption`: aar with Android encryption specific classes.
* `hammock-sync-datastore-javase`: jar with Java SE specific classes.
