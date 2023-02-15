/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Copyright © 2013 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.hammock.sync.util;

import org.hammock.sync.documentstore.DocumentBody;
import org.hammock.sync.documentstore.DocumentBodyFactory;
import org.hammock.sync.documentstore.encryption.NullKeyProvider;
import org.hammock.sync.internal.util.Misc;
import org.hammock.sync.internal.query.QueryImpl;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.sqlite.SQLDatabaseFactory;
import org.hammock.sync.internal.sqlite.SQLDatabaseQueue;

import org.apache.commons.io.FileUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;

public class TestUtils {

    private final static String DATABASE_FILE_EXT = ".sqlite4java";

    public static DocumentBody createBDBody(String filePath) throws IOException {
        byte[] data = FileUtils.readFileToByteArray(TestUtils.loadFixture(filePath));
        return DocumentBodyFactory.create(data);

    }

    public static SQLDatabase createEmptyDatabase(String database_dir, String database_file) throws IOException, SQLException {
        File dbFile = new File(database_dir + File.separator + database_file + DATABASE_FILE_EXT);
        FileUtils.touch(dbFile);
        return SQLDatabaseFactory.openSQLDatabase(dbFile, new NullKeyProvider());
    }

    public static void deleteDatabaseQuietly(SQLDatabase database) {
        try {
            if (database != null) {
                if (database.isOpen()) {
                    database.close(); // Close before deleting
                }
                FileUtils.deleteQuietly(new File(database.filename));
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public static SQLDatabaseQueue getDBQueue(QueryImpl indexManager) throws Exception {
        Class clazz =  QueryImpl.class;
        Field dbQueue = clazz.getDeclaredField("dbQueue");
        dbQueue.setAccessible(true);
        return (SQLDatabaseQueue) dbQueue.get(indexManager);
    }

    public static String createTempTestingDir(String dirPrefix) {
        String tempPath = String.format(
                "%s_%s",
                dirPrefix,
                UUID.randomUUID()
        );
        File f = new File(
                FileUtils.getTempDirectory().getAbsolutePath(),
                tempPath);
        f.mkdirs();
        return f.getAbsolutePath();
    }

    public static void deleteTempTestingDir(String path) {
        FileUtils.deleteQuietly(new File(path));
    }

    public static String createTempFile(String dir, String file) throws IOException {
        File f = new File(dir + File.separator + file);
        FileUtils.touch(f);
        return f.getAbsolutePath();
    }

    // iterate through both streams byte-for-byte and check they are equal
    // exit false if we get to the end of one stream before the other (they are different lengths)
    // or if two bytes at the same point in the streams aren't equal
    public static boolean streamsEqual(InputStream is1, InputStream is2){
        int c1, c2;
        boolean equal = true;
        try {
            while ((c1 = is1.read()) != -1) {
                c2 = is2.read();
                // % is 'any' metacharacter
                if (c1 == '%') {
                    continue;
                }
                if (c1 != c2) {
                    equal = false;
                    break;
                }
            }
            if (is2.read() != -1) {
                // more bytes in the 2nd stream
                return false;
            }
        } catch (IOException ioe) {
            return false;
        }
        return equal;
    }

    /**
     * Load a test fixture, by either suffixing a path to the external storage directory for an android
     * emulator or device, or directly going to the fixture directory in the working directory to
     * create a file object from which to read a fixture.
     * @param fileName the name of a fixture to read
     * @return {@link java.io.File} object An file object representing a fixture on disk
     */
   public static File loadFixture(String fileName){
       if(Misc.isRunningOnAndroid()){
           //yay more reflection goes here

           try {
               Class<?> env = Class.forName("android.os.Environment");
               Method externalStorageMethod = env.getMethod("getExternalStorageDirectory");

               File sdcard = (File) externalStorageMethod.invoke(null);
               return new File(sdcard, fileName);
           } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException  e ){
               e.printStackTrace();
               return null;
           }

       }else {
           //just return the new File object
           return new File(fileName);

       }
   }

}
