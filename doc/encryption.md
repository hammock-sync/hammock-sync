# DocumentStore encryption

Android’s document store now supports encryption of all data inside your database 
using 256-bit AES: JSON documents, Query indexes and attachments.

JSON documents and Query indexes are stored in SQLite databases. We use
SQLCipher to encrypt the data in those databases. Attachment data is 
stored as binary blobs on the filesystem. We use the Android JCE implementation
for this, using 256-bit AES in CBC mode. Each distinct file blob gets its
own IV, stored with the file on disk.

## Using Encryption in your Application

If you plan to use encryption, first you need to include the Hammock Sync Datastore Encryption 
artifact in your project dependencies.

```gradle
   implementation 'org.hammock.sync:datastore-android.encryption:<version>'
```

Our implementation uses [SQLCipher Community Edition][1] as encryption support for SQLite databases 
in Android. Hammock Sync uses the AAR distribution that contains the `.jar` and `.so` binary files required 
by the encryption library.

[1]: https://www.zetetic.net/sqlcipher/open-source/

Once you've included the proper Hammock Sync artifact in your app's build, you need to perform some 
set up in code:

1.	Load SQLCipher library.  This line needs to be done before any database calls:
    
    ```java
        System.loadLibrary("sqlcipher");
    ```

2.	With encryption, two parameters are required in the `getInstance` call: the 
    application storage path and a `KeyProvider` object.  The `KeyProvider` interface 
    can be instantiated with the `SimpleKeyProvider` class, which just provides a
    developer or user set encryption key.  Create a new SimpleKeyProvider 
    by providing a 256-bit key as a `byte` array in the constructor. For example:
    
    ```java
    // Security risk here: hard-coded key.
    // We recommend using java.util.SecureRandom to generate your key, then
    // storing securely or retrieving it from elsewhere. 
    // Or use AndroidKeyProvider, which does this for you.
    byte[] key = "testAKeyPasswordtestAKeyPassword".getBytes();  
    KeyProvider keyProvider = new SimpleKeyProvider(key); 

    File path = getApplicationContext().getDir("documentstores", MODE_PRIVATE);

    DocumentStore ds = DocumentStore.getInstance(new File(path, "my_documentstore"), keyProvider);
    ```
    
    Note: The key _must_ be 32 bytes (256-bit key). `"testAKeyPasswordtestAKeyPassword"`
    happens to meet this requirement when `getBytes()` is called, which makes this
    example more readable.
    
    See the next section for a secure key provider, which generates and protects keys
    for you.

## Secure Key Generation and Storage using AndroidKeyProvider

The SimpleKeyProvider does not provide proper management and storage of the key.  
If this is required, use the `AndroidKeyProvider`. This class handles 
generating a strong encryption key protected using the provided password. The
key data is encrytped and saved into the application's `SharedPreferences`. The 
constructor requires the Android context, a user-provided password, and an 
identifier to access the saved data in SharedPreferences.

Example:

```java
KeyProvider keyProvider = new AndroidKeyProvider(context, 
        "ASecretPassword", "AnIdentifier");
DocumentStore ds = DocumentStore.getInstance(new File(path, "my_documentstore"), keyProvider);
```

One example of an identifier might be if multiple users share the same
device, the identifier can be used on a per user basis to store a key
to their individual database.

Right now, a different key is stored under each identifier, so one cannot
use identifiers to allow different users access to the same encrypted
database -- each user would get a different encryption key, and would
not be able to decrypt the database. For this use case, currently 
a custom implementation of `KeyProvider` is required.

## Full code sample:

```java
protected void onCreate(Bundle savedInstanceState) {
 
    super.onCreate(savedInstanceState);
 
    SQLiteDatabase.loadLibs(this);
 
    // Get a DocumentStore instance with encryption using 
    // application internal storage path and a key
    File path = getApplicationContext().getDir("documentstores", MODE_PRIVATE);
    KeyProvider keyProvider = 
            new SimpleKeyProvider("testAKeyPasswordtestAKeyPassword".getBytes());
 
    DocumentStore ds = null;
    try {
        ds = DocumentStore.getInstance(new File(path, "my_documentstore"), keyProvider);
    } catch (DocumentStoreNotOpenedException e) {
        e.printStackTrace();
    }
        
    // ...
````

## Licence

We use [JCE][JCE] library to encrypt the attachments before
saving to disk. There should be no licencing concerns for using JCE.

Databases are automatically encrypted with
[SQLCipher][SQLCipher]. SQLCipher requires including its
[BSD-style license][BSD-style license] and copyright in your application and
documentation. Therefore, if you use document store encryption in your application, 
please follow the instructions mentioned [here](https://www.zetetic.net/sqlcipher/open-source/).

[SQLCipher]: https://www.zetetic.net/sqlcipher/
[JCE]: http://developer.android.com/reference/javax/crypto/package-summary.html
[BSD-style license]:https://www.zetetic.net/sqlcipher/license/
