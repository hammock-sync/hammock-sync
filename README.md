# Hammock Sync Android

**A CouchDB Sync Client for Android**

## Overview

Hammock Sync is a Java library designed to facilitate synchronization between CouchDB
databases and Android applications. This library aims to provide seamless integration and reliable 
data sync capabilities.

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

## Features

- **Automatic synchronization** between CouchDB and local databases.
- **Conflict resolution** for handling data discrepancies.
- **Lightweight and efficient** for mobile applications.
- **Easy to integrate** with existing Android projects.

## Using in your project

The library is currently published via maven central repository. Using it in your project should
be as simple as adding it as a dependency via [maven][maven] or [gradle][gradle].

```gradle
dependencies {
    implementation 'org.hammock-sync:datastore-android:1.0.0'
}
```

[maven]: http://maven.apache.org/
[gradle]: http://www.gradle.org/

There are currently four artifacts for the datastore, two jar and two aar:

* `datastore-core`: jar with the main datastore classes. Should not be included as a direct dependency in your projects.
* `datastore-android`: aar with Android specific classes.
* `datastore-android-encryption`: aar with Android encryption specific classes.
* `datastore-javase`: jar with Java SE specific classes.

Select the one that best suits your project type:
* `datastore-android` for Android applications.
* `datastore-android-encryption` for Android applications with encryption.
* `datastore-javase` for Java applications.


## Usage

Please check sample project for usage.

## Contributing

We welcome contributions! Please see our [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to get started.

## License

Hammock Sync is released under the MIT License. See the [LICENSE](LICENSE) file for more details.

## Support

For support and questions, please open an issue in this repository or contact us at hammock-sync@lksnext.com.
