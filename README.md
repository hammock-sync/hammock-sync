# Hammock Sync Android

**A CouchDB Sync Client for Android**

## Overview

Hammock Sync is a Java library for synchronizing CouchDB databases with Android
and Java applications. It provides local JSON storage, indexing, querying, and
application-controlled data synchronization.

**Applications use Hammock Sync to store, index and query local JSON data on a
device and to synchronise data between many devices. Synchronisation is under
the control of the application, rather than being controlled by the underlying
system. Conflicts are also easy to manage and resolve, either on the local
device or in the remote database.**

Hammock Sync is an [Apache CouchDB&trade;][acdb]
replication-protocol-compatible datastore for devices that do not want or need
to run a full CouchDB instance. It was originally built by
[Cloudant](https://cloudant.com), building on the work of many others, and is
now maintained and modernized by the Hammock Sync project.

The Hammock Sync project is a derivative work from Cloudant Sync for Android,
which has been abandoned by its original authors. Hammock Sync continues this
CouchDB sync client for Android and Java.

Hammock Sync is distributed under the [Apache License 2.0][license]. It retains
the original copyright notices for elements derived from the original project.

[license]: LICENSE
[acdb]: https://couchdb.apache.org/

The API is quite different from CouchDB's; we retain the
[MVCC](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) data
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
    implementation 'org.hammock-sync:datastore-android:1.1.0'
}
```

[maven]: https://maven.apache.org/
[gradle]: https://gradle.org/

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

## Supported build and platform versions

| Component | Supported version |
| --- | --- |
| Android | API 29 and newer |
| Java SE artifacts | Java 8 bytecode |
| Android Java compatibility | Java 11 |
| Build JDK | Java 17 |
| Gradle | 8.14.3 |
| Android Gradle Plugin | 8.13.2 |
| Compile/target SDK | 36 |
| CouchDB | 2.3.1, 3.4.3, and 3.5.1 validated by CI |

## License

Hammock Sync is released under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## Support

For support and questions, please open an issue in this repository or contact us at hammock-sync@lksnext.com.
