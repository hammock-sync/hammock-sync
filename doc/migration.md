# Migrating from Cloudant Sync for Android

## Package changes

All classes are now in the `org.hammock.sync` package.

## Changes to Replication Policies
The support for Android Replication Policies has been removed from Hammock Sync. Please use 
Android native support for programming background jobs like WokrManager[1]

[1] https://developer.android.com/reference/androidx/work/WorkManager

