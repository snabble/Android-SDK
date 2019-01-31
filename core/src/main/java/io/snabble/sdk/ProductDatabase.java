package io.snabble.sdk;


import android.app.Application;
import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.Formatter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.utils.Downloader;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.StringNormalizer;

public class ProductDatabase {
    private static final int SCHEMA_VERSION_MAJOR_COMPATIBILITY = 1;

    private static final String METADATA_KEY_SCHEMA_VERSION_MAJOR = "schemaVersionMajor";
    private static final String METADATA_KEY_SCHEMA_VERSION_MINOR = "schemaVersionMinor";
    private static final String METADATA_KEY_REVISION = "revision";
    private static final String METADATA_KEY_PROJECT = "project";
    private static final String METADATA_KEY_LAST_UPDATE_TIMESTAMP = "app_lastUpdateTimestamp";

    private SQLiteDatabase db;
    private Product.Type[] productTypes = Product.Type.values();

    private long revisionId;

    private final Object dbLock = new Object();
    private String dbName;

    private List<OnDatabaseUpdateListener> onDatabaseUpdateListeners = new CopyOnWriteArrayList<>();
    private Date lastUpdateDate;
    private int schemaVersionMajor;
    private int schemaVersionMinor;
    private boolean generateSearchIndex;

    private Project project;
    private Application application;
    private ProductDatabaseDownloader productDatabaseDownloader;

    private Handler handler = new Handler(Looper.getMainLooper());
    private ProductApi productApi;

    ProductDatabase(Project project, String name, boolean generateSearchIndex) {
        this.project = project;
        this.application = Snabble.getInstance().getApplication();
        this.dbName = name;

        this.generateSearchIndex = generateSearchIndex;

        this.productDatabaseDownloader = new ProductDatabaseDownloader(project, this);
        this.productApi = new ProductApi(project);

        if (!open()) {
            Logger.i("Product database is missing. Offline products are not available.");
        }
    }

    private ProductDatabase(Project project, String name) {
        this(project, name, false);
    }

    private boolean open() {
        if (dbName == null) {
            return true;
        }

        synchronized (dbLock) {
            File file = application.getDatabasePath(dbName);

            try {
                if (!file.exists()) {
                    return false;
                }

                db = SQLiteDatabase.openDatabase(file.getAbsolutePath(),
                        null, SQLiteDatabase.OPEN_READWRITE);
                // since Android 9 the default WAL mode is "normal" instead of "full"
                // which interferes with full database updates using a temp file
                // so we disable it here
                db.disableWriteAheadLogging();

                try {
                    revisionId = Long.parseLong(getMetaData(METADATA_KEY_REVISION));
                } catch (NumberFormatException e) {
                    revisionId = -1;
                }

                schemaVersionMajor = Integer.parseInt(getMetaData(METADATA_KEY_SCHEMA_VERSION_MAJOR));
                schemaVersionMinor = Integer.parseInt(getMetaData(METADATA_KEY_SCHEMA_VERSION_MINOR));

                if (schemaVersionMajor == 1 && schemaVersionMinor < 18) {
                    Logger.d("Database is too old, deleting local database");
                    delete();
                    return false;
                }

                createFTSIndexIfNecessary();
                parseLastUpdateTimestamp();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void close() {
        synchronized (dbLock) {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Loads the a database from a file located int the assets folder.
     * <p>
     * If the loaded database is older then the one currently used, nothing will happen.
     * <p>
     * Make sure to provide the correct revision, major or minor, or else the database may be copied
     * even if its older then the one currently used.
     */
    public void loadDatabaseBundle(String assetPath, long revision, int major, int minor) {
        try {
            loadDatabaseBundle(application.getResources().getAssets().open(assetPath), revision, major, minor);
        } catch (IOException e) {
            Logger.e("Could not load database from bundle: " + e.toString());
        }
    }

    /**
     * Loads the a database from an InputStream.
     * <p>
     * If the loaded database is older then the one currently used, nothing will happen.
     * <p>
     * Make sure to provide the correct revision, major or minor, or else the database may be copied
     * even if its older then the one currently used.
     */
    public void loadDatabaseBundle(InputStream inputStream, long revision, int major, int minor) {
        boolean bundleHasNewerSchema = major != -1 && minor != -1
                && (major > schemaVersionMajor || minor > schemaVersionMinor);

        String dbProject = getMetaData(METADATA_KEY_PROJECT);
        boolean isOtherProject = !project.getId().equals(dbProject);

        if (revisionId < revision || bundleHasNewerSchema || isOtherProject) {
            close();

            if (copyDb(inputStream)) {
                if (bundleHasNewerSchema) {
                    Logger.d("Bundled product database has newer schema (%d.%d -> %d.%d)",
                            schemaVersionMajor, schemaVersionMinor,
                            major, minor);
                } else if (isOtherProject) {
                    Logger.d("Bundled product database has different projectId (%s -> %s)",
                            dbProject, project.getId());
                } else {
                    Logger.d("Bundled product database is newer (%d -> %d)",
                            revisionId, revision);
                }
            } else {
                Logger.e("Could not copy database from assets");
            }

            open();
        } else {
            Logger.d("Loaded product database revision %d, schema version %d.%d, %s",
                    revisionId, schemaVersionMajor, schemaVersionMinor,
                    Formatter.formatFileSize(application, size()));
        }
    }

    private void putMetaData(String key, String value) {
        synchronized (dbLock) {
            if (db != null && db.isOpen()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("key", key);
                contentValues.put("value", value);
                db.replace("metadata", null, contentValues);
            }
        }
    }

    private String getMetaData(String key) {
        Cursor cursor = rawQuery("SELECT value FROM metadata WHERE key = ?", new String[]{
                key
        }, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            String value = cursor.getString(0);
            cursor.close();
            return value;
        }

        return null;
    }

    private void parseLastUpdateTimestamp() {
        String lastDownloadTimestamp = getMetaData(METADATA_KEY_LAST_UPDATE_TIMESTAMP);
        if (lastDownloadTimestamp != null) {
            lastUpdateDate = new Date(Long.parseLong(lastDownloadTimestamp));
        } else {
            try {
                PackageInfo packageInfo = application.getPackageManager()
                        .getPackageInfo(application.getPackageName(), 0);

                updateLastUpdateTimestamp(packageInfo.lastUpdateTime);
            } catch (PackageManager.NameNotFoundException ex) {
                //this cant happen
                updateLastUpdateTimestamp(0);
            }
        }
    }

    void updateLastUpdateTimestamp(long timestamp) {
        putMetaData(METADATA_KEY_LAST_UPDATE_TIMESTAMP, String.valueOf(timestamp));

        try {
            lastUpdateDate = new Date(timestamp);
            Logger.d("Updating last update timestamp: %s", lastUpdateDate.toString());
        } catch (AssertionError e) {
            // Some Android 8 and 8.1 builds are faulty and throw an AssertionError when
            // accessing time zone information
            //
            // Since this is only used for logging and we are fine with catching the error
            // and not providing an alternative implementation
            //
            // see https://issuetracker.google.com/issues/110848122
        }
    }

    /**
     * The current revision of the database. This number is increasing every time new data is provided to the backend.
     */
    public long getRevisionId() {
        return revisionId;
    }

    /**
     * The current major schema version. Increases indicate backwards incompatible changes.
     */
    public long getSchemaVersionMajor() {
        return schemaVersionMajor;
    }

    /**
     * The current minor schema version. Increases indicate backwards compatible changes.
     */
    public long getSchemaVersionMinor() {
        return schemaVersionMinor;
    }

    /**
     * @return The last time the database was updated.
     */
    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * Applies a delta update from an input stream. The input stream should contain a series
     * of sql statements.
     * <p>
     * If the input stream contains invalid sql statements no changes will be made to the database.
     * <p>
     * While updating, the database can still be queried for reads.
     * <p>
     * If a read error occurs, an IOException will be thrown.
     * <p>
     * Returns true if successful, false otherwise
     */
    synchronized void applyDeltaUpdate(InputStream inputStream) throws IOException {
        if (dbName == null) {
            return;
        }

        Logger.d("Applying delta update...");

        long fromRevisionId = getRevisionId();
        long time = SystemClock.elapsedRealtime();

        File dbFile = application.getDatabasePath(dbName);
        File tempDbFile = application.getDatabasePath("_" + dbName);

        if (!deleteDatabase(tempDbFile)) {
            Logger.e("Could not recalculate delta update: Could not delete temp database");
            throw new IOException();
        }

        try {
            FileUtils.copyFile(dbFile, tempDbFile);
        } catch (IOException e) {
            project.logErrorEvent("Could not copy db to temp file: %s", e.getMessage());
            throw e;
        }

        SQLiteDatabase tempDb = SQLiteDatabase.openOrCreateDatabase(tempDbFile, null);

        Scanner scanner = new Scanner(inputStream, "UTF-8");

        //delta update statements are splitted by ;\n\n - occurrences of ;\n\n in strings are
        //escaped replaced by the backend - splitting by just ; is not enough because of eventual
        //occurrences in database rows
        scanner.useDelimiter(";\n\n");

        try {
            tempDb.beginTransaction();
        } catch (SQLiteException e) {
            project.logErrorEvent("Could not recalculate delta update: Could not access temp database");
            throw new IOException();
        }

        while (scanner.hasNext()) {
            try {
                String line = scanner.next();

                //Scanner catches IOExceptions and stores the last thrown exception in ioException()
                //because we want to handle possible IOExceptions we throw them here again if they
                //occurred
                IOException lastException = scanner.ioException();
                if (lastException != null) {
                    project.logErrorEvent("Could not recalculate delta update: %s", lastException.getMessage());
                    tempDb.close();
                    deleteDatabase(tempDbFile);
                    throw lastException;
                }

                tempDb.execSQL(line);
            } catch (SQLiteException e) {
                //code 0 = "not an error" - statements like empty strings are causing that exception
                //which we want to allow
                if (!e.getMessage().contains("code 0")) {
                    project.logErrorEvent("Could not recalculate delta update: %s", e.getMessage());
                    tempDb.close();
                    deleteDatabase(tempDbFile);
                    throw new IOException();
                }
            }
        }

        try {
            tempDb.setTransactionSuccessful();
            tempDb.endTransaction();
        } catch (SQLiteException e) {
            project.logErrorEvent("Could not recalculate delta update: Could not finish transaction on temp database");
            throw new IOException();
        }

        //delta updates are making the database grow larger and larger, so we vacuum here
        //to keep the database as small as possible
        long vacuumTime = SystemClock.elapsedRealtime();
        try {
            tempDb.execSQL("VACUUM");
        } catch (SQLiteException e) {
            project.logErrorEvent("Could not recalculate delta update: %s", e.getMessage());
            deleteDatabase(tempDbFile);
            throw new IOException();
        }

        long vacuumTime2 = SystemClock.elapsedRealtime() - vacuumTime;
        Logger.d("VACUUM took %d ms", vacuumTime2);

        tempDb.close();

        try {
            swap(tempDbFile);
        } catch (IOException e) {
            project.logErrorEvent("Could not recalculate delta update: %s", e.getMessage());
            throw new IOException();
        }

        long time2 = SystemClock.elapsedRealtime() - time;
        Logger.d("Delta update (%d -> %d) took %d ms", fromRevisionId, getRevisionId(), time2);
    }

    /**
     * Applies a full update from an input stream. The input stream should contain a full and valid
     * SQLite3 Database.
     * <p>
     * If a read error occurs, an IOException will be thrown.
     */
    synchronized void applyFullUpdate(InputStream inputStream) throws IOException {
        if (dbName == null) {
            return;
        }

        Logger.d("Applying full update...");

        long time = SystemClock.elapsedRealtime();

        File tempDbFile = application.getDatabasePath("_" + dbName);

        if (!deleteDatabase(tempDbFile)) {
            project.logErrorEvent("Could not recalculate full update: Could not delete old database download");
            throw new IOException();
        }

        tempDbFile.getParentFile().mkdirs();

        try {
            IOUtils.copy(inputStream, new FileOutputStream(tempDbFile));
            swap(tempDbFile);
        } catch (IOException e) {
            project.logErrorEvent("Could not recalculate full update: %s", e.getMessage());
            throw e;
        }

        long time2 = SystemClock.elapsedRealtime() - time;
        Logger.d("Full update took %d ms", time2);
    }

    private void createFTSIndexIfNecessary() {
        if (generateSearchIndex) {
            long time = SystemClock.elapsedRealtime();

            synchronized (dbLock) {
                Cursor cursor;
                cursor = rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='searchByName'", null, null);
                boolean hasFTS = cursor != null && cursor.getCount() == 1;
                if (cursor != null) {
                    cursor.close();
                }

                if (!hasFTS) {
                    db.beginTransaction();

                    exec("DROP TABLE IF EXISTS searchByName");
                    exec("CREATE VIRTUAL TABLE searchByName USING fts4(sku TEXT, foldedName TEXT)");

                    ContentValues contentValues = new ContentValues(2);
                    cursor = rawQuery("SELECT sku, name FROM products", null, null);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            contentValues.clear();
                            contentValues.put("sku", cursor.getString(0));
                            contentValues.put("foldedName", StringNormalizer.normalize(cursor.getString(1)));
                            db.insert("searchByName", null, contentValues);
                        }
                        cursor.close();
                    } else {
                        Logger.d("Could not create FTS4 index, no products");
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                    Logger.d("Created FTS4 index in " + (SystemClock.elapsedRealtime() - time) + " ms");
                } else {
                    Logger.d("Already has FTS4 index");
                }
            }
        }
    }

    private Cursor rawQuery(String sql, String[] args, CancellationSignal cancellationSignal) {
        if (db == null) {
            return null;
        }

        long time = SystemClock.elapsedRealtime();
        Cursor cursor;

        synchronized (dbLock) {
            try {
                cursor = db.rawQuery(sql, args, cancellationSignal);

                // query executes when we call the first function that needs data, not on db.rawQuery
                int count = cursor.getCount();

                long time2 = SystemClock.elapsedRealtime() - time;
                if (time2 >= 16) {
                    Logger.d("Query performance warning (%d ms, %d rows) for SQL: %s",
                            time2, count, bindArgs(sql, args));
                }
            } catch (Exception e) {
                // query could not be executed
                Logger.e(e.toString());
                return null;
            }
        }

        return cursor;
    }

    /**
     * Updates the current database in the background by requesting new data from the backend.
     * <p>
     * Updates can be either delta updates or full updates depending on how big the difference between
     * the last update was.
     * <p>
     * While updating, the database can still be queried for data, after the update completes calls to the database
     * return the updated data.
     *
     * Note that database updates are usually very cheap and do not transmit data that is already on your device.
     *
     * If the database is not present or schematic changes are done that can not be resolved via a delta update
     * a full update is needed.
     */
    public void update() {
        update(null);
    }

    /**
     * Updates the current database in the background by requesting new data from the backend.
     * <p>
     * Updates can be either delta updates or full updates depending on how big the difference between
     * the last update was.
     * <p>
     * While updating, the database can still be queried for data, after the update completes calls to the database
     * return the updated data.
     *
     * Note that database updates are usually very cheap and do not transmit data that is already on your device.
     *
     * If the database is not present or schematic changes are done that can not be resolved via a delta update
     * a full update is needed.
     *
     * @param callback A {@link UpdateCallback} that returns success when the operation is successfully completed.
     *                 Or error() in case a network error occurred.
     */
    public void update(final UpdateCallback callback) {
        update(callback, false);
    }

    /**
     * Updates the current database in the background by requesting new data from the backend.
     * <p>
     * Updates can be either delta updates or full updates depending on how big the difference between
     * the last update was.
     * <p>
     * While updating, the database can still be queried for data, after the update completes calls to the database
     * return the updated data.
     *
     * Note that database updates are usually very cheap and do not transmit data that is already on your device.
     *
     * If the database is not present or schematic changes are done that can not be resolved via a delta update
     * a full update is needed.
     *
     * @param callback        A {@link UpdateCallback} that returns success when the operation is successfully completed.
     *                        Or error() in case a network error occurred. Can be null.
     * @param deltaUpdateOnly set to true if you want to only update when the update would be an delta update
     */
    public void update(final UpdateCallback callback, boolean deltaUpdateOnly) {
        if (dbName == null) {
            return;
        }

        productDatabaseDownloader.invalidate();
        productDatabaseDownloader.update(new Downloader.Callback() {
            @Override
            protected void onDataLoaded(boolean wasStillValid) {
                update();
            }

            @Override
            protected void onError() {
                if (productDatabaseDownloader.wasSameRevision()) {
                    update();
                } else {
                    if (callback != null) {
                        callback.error();
                    }
                }
            }

            private void update() {
                updateLastUpdateTimestamp(System.currentTimeMillis());
                notifyOnDatabaseUpdated();

                if (callback != null) {
                    callback.success();
                }

                project.getShoppingCart().update();
            }
        }, deltaUpdateOnly);
    }

    /**
     * Cancels a database update, if one is currently running.
     */
    public void cancelUpdate() {
        productDatabaseDownloader.cancel();
    }

    /**
     * @return true if a database update is currently running, false otherwise.
     */
    public boolean isUpdating() {
        return productDatabaseDownloader.isLoading();
    }

    private boolean deleteDatabase(File dbFile) {
        if (dbFile.exists()) {
            return application.deleteDatabase(dbFile.getName());
        }
        return true;
    }


    /**
     * Closes and deletes the locally stored database and falls back to online only mode.
     */
    public void delete() {
        if (db != null) {
            close();
            application.deleteDatabase(dbName);
            db = null;
            revisionId = -1;
            schemaVersionMinor = -1;
            schemaVersionMajor = -1;

            Logger.d("Deleted database: " + dbName);
        }
    }

    /**
     * @return Size of the database in bytes.
     */
    public long size() {
        if (db == null) {
            return 0;
        }

        File dbFile = application.getDatabasePath(dbName);
        return dbFile.length();
    }

    private void swap(File otherDbFile) throws IOException {
        boolean ok = true;

        ProductDatabase otherDb = new ProductDatabase(project, otherDbFile.getName());
        try {
            otherDb.open();

            if (!otherDb.verify()) {
                ok = false;
            }
        } catch (Exception e) {
            ok = false;
        }

        otherDb.close();

        if (!ok) {
            Logger.e("Could not swap database: malformed database or unknown schema version");
            application.deleteDatabase(otherDbFile.getName());
            return;
        }

        synchronized (dbLock) {
            close();

            File dbFile = application.getDatabasePath(dbName);

            if (!dbFile.exists() || application.deleteDatabase(dbFile.getName())) {
                FileUtils.moveFile(otherDbFile, dbFile);
                application.deleteDatabase(otherDbFile.getName());
            }

            boolean openOk;
            try {
                openOk = open();
            } catch (SQLiteException e) {
                openOk = false;
            }

            if (!openOk) {
                Logger.e("Could not open database after applying full update, falling back to online only mode");
            }
        }
    }

    private boolean verify() {
        long time = SystemClock.elapsedRealtime();

        if (!db.isDatabaseIntegrityOk()) {
            return false;
        }

        try {
            if (schemaVersionMajor != SCHEMA_VERSION_MAJOR_COMPATIBILITY) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        long time2 = SystemClock.elapsedRealtime() - time;
        Logger.d("Verify successful in %d ms", time2);

        return true;
    }

    private boolean copyDb(InputStream inputStream) {
        if (dbName == null || inputStream == null) {
            return false;
        }

        long time = SystemClock.elapsedRealtime();

        try {
            File outputFile = application.getDatabasePath(dbName);
            //noinspection ResultOfMethodCallIgnored
            outputFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(outputFile);
            IOUtils.copy(inputStream, fos);
        } catch (IOException e) {
            return false;
        }

        long time2 = SystemClock.elapsedRealtime() - time;
        Logger.d("File copy took %d ms", time2);
        return true;
    }

    /**
     * Returns the current product where the cursor is currently pointing at.
     * Does not modify the state of the cursor.
     * <p>
     * The cursor should be a Cursor that is returned from the functions
     * <p>
     * {@link ProductDatabase#searchByCode(String, CancellationSignal)}
     * or
     * {@link ProductDatabase#searchByFoldedName(String, CancellationSignal)}
     */
    public Product productAtCursor(Cursor cursor) {
        Product.Builder builder = new Product.Builder();

        String[] lookupCodes = null;
        String[] transmissionCodes = null;
        String[] codeEncodingUnits = null;
        String[] templates = null;

        String sku = anyToString(cursor, 0);

        builder.setSku(sku)
                .setName(cursor.getString(1))
                .setDescription(cursor.getString(2))
                .setImageUrl(ensureNotNull(cursor.getString(3)));

        String depositSku = anyToString(cursor, 4);

        builder.setIsDeposit(cursor.getInt(5) != 0)
                .setType(productTypes[cursor.getInt(6)]);

        builder.setDepositProduct(findBySku(depositSku));

        String codes = cursor.getString(7);
        if (codes != null) {
            lookupCodes = codes.split(",");
        }

        builder.setSubtitle(cursor.getString(8));

        builder.setSaleRestriction(decodeSaleRestriction(cursor.getLong(9)));
        builder.setSaleStop(cursor.getInt(10) != 0);

        builder.setBundleProducts(findBundlesOfProduct(builder.build()));

        if (lookupCodes != null) {
            String transmissionCodesStr = cursor.getString(11);
            if (transmissionCodesStr != null) {
                transmissionCodes = transmissionCodesStr.split(",", -1);
            }
        }


        String referenceUnit = cursor.getString(12);
        if (referenceUnit != null) {
            Unit unit = Unit.fromString(referenceUnit);
            builder.setReferenceUnit(unit);

            if (unit == Unit.PIECE) {
                builder.setType(Product.Type.Article);
            }
        }

        String encodingUnit = cursor.getString(13);
        if (encodingUnit != null) {
            Unit unit = Unit.fromString(encodingUnit);
            builder.setEncodingUnit(unit);
        }

        if (lookupCodes != null) {
            String encodingUnitsStr = cursor.getString(14);
            if (encodingUnitsStr != null) {
                codeEncodingUnits = encodingUnitsStr.split(",", -1);
            }

            String templatesStr = cursor.getString(15);
            if (templatesStr != null) {
                templates = templatesStr.split(",", -1);
            }
        }

        if (lookupCodes != null) {
            Product.Code[] productCodes = new Product.Code[lookupCodes.length];
            for (int i = 0; i < productCodes.length; i++) {
                String lookupCode = lookupCodes[i];
                String transmissionCode = null;
                Unit codeEncodingUnit = null;
                CodeTemplate template = null;

                if (transmissionCodes != null) {
                    String tc = transmissionCodes[i];
                    if (!tc.equals("")) {
                        transmissionCode = tc;
                    }
                }

                if (codeEncodingUnits != null) {
                    codeEncodingUnit = Unit.fromString(codeEncodingUnits[i]);
                }

                if (templates != null) {
                    String templateStr = templates[i];
                    for (CodeTemplate codeTemplate : Snabble.getInstance().getCodeTemplates()) {
                        if (codeTemplate.getName().equals(templateStr)) {
                            template = codeTemplate;
                        }
                    }
                }

                String templateName = template != null ? template.getName() : null;
                productCodes[i] = new Product.Code(lookupCode, transmissionCode, templateName, codeEncodingUnit);
            }

            builder.setScannableCodes(productCodes);
        }

        Shop shop = project.getCheckedInShop();

        if(!queryPrice(builder, sku, shop)) {
            queryPrice(builder, sku, null);
        }

        return builder.build();
    }

    private boolean queryPrice(Product.Builder builder, String sku, Shop shop) {
        String id = shop != null ? shop.getId() : "";
        String priceQuery = "SELECT listPrice, discountedPrice, basePrice FROM prices " +
                "WHERE pricingCategory = ifnull((SELECT pricingCategory FROM shops WHERE shops.id = ?), '0') AND sku = ?";

        Cursor priceCursor = rawQuery(priceQuery, new String[]{id, sku}, null);

        if (priceCursor != null && priceCursor.getCount() > 0) {
            priceCursor.moveToFirst();
            builder.setPrice(priceCursor.getInt(0));
            builder.setDiscountedPrice(priceCursor.getInt(1));
            builder.setBasePrice(priceCursor.getString(2));
            priceCursor.close();
            return true;
        }

        return false;
    }

    private Product.SaleRestriction decodeSaleRestriction(long encodedValue) {
        long type = encodedValue & 0xFF;
        long value = encodedValue >> 8;

        return Product.SaleRestriction.fromDatabaseField(type, value);
    }

    private String anyToString(Cursor cursor, int index) {
        switch (cursor.getType(index)) {
            case Cursor.FIELD_TYPE_INTEGER:
                return String.valueOf(cursor.getLong(index));
            case Cursor.FIELD_TYPE_FLOAT:
                return String.valueOf(cursor.getFloat(index));
            case Cursor.FIELD_TYPE_STRING:
                return cursor.getString(index);
            default:
                return null;
        }
    }

    private String ensureNotNull(String in) {
        if (in == null) {
            return "";
        }

        return in;
    }

    private String productSqlString(String appendSql, boolean distinct) {
        String sql = "SELECT " + (distinct ? "DISTINCT " : "") +
                "p.sku," +
                "p.name," +
                "p.description," +
                "p.imageUrl," +
                "p.depositSku," +
                "p.isDeposit," +
                "p.weighing," +
                "(SELECT group_concat(s.code) FROM scannableCodes s WHERE s.sku = p.sku)," +
                "p.subtitle" +
                ",p.saleRestriction"+
                ",p.saleStop" +
                ",(SELECT group_concat(ifnull(s.transmissionCode, \"\")) FROM scannableCodes s WHERE s.sku = p.sku)" +
                ",p.referenceUnit" +
                ",p.encodingUnit" +
                ",(SELECT group_concat(ifnull(s.encodingUnit, \"\")) FROM scannableCodes s WHERE s.sku = p.sku)" +
                ",(SELECT group_concat(ifnull(s.template, \"\")) FROM scannableCodes s WHERE s.sku = p.sku)" +
                " FROM products p "
                + appendSql;

        return sql;
    }

    private Cursor productQuery(String appendSql, String[] args, boolean distinct, CancellationSignal cancellationSignal) {
        return rawQuery(productSqlString(appendSql, distinct), args, cancellationSignal);
    }

    public boolean isAvailableOffline() {
        return db != null;
    }

    public boolean isUpToDate() {
        if (lastUpdateDate != null) {
            long time = lastUpdateDate.getTime();
            long currentTime = new Date().getTime();
            long t = time + Snabble.getInstance().getConfig().maxProductDatabaseAge;
            return t > currentTime;
        }

        return false;
    }

    private Cursor productQuery(String appendSql, String[] args, boolean distinct) {
        return productQuery(appendSql, args, distinct, null);
    }

    /**
     * Find a product by its name. Matching is normalized, so "Apple" finds also "apple".
     *
     * @param name The name of the product.
     * @return The first product matching the name, otherwise null if no product was found.
     */
    public Product findByName(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }

        StringNormalizer.normalize(name);

        Cursor cursor = productQuery("JOIN searchByName ns ON ns.sku = p.sku " +
                "WHERE ns.foldedName MATCH ? LIMIT 1", new String[]{
                name
        }, false);

        return getFirstProductAndClose(cursor);
    }

    private void exec(String sql) {
        Cursor cursor = rawQuery(sql, null, null);
        if (cursor != null) {
            cursor.close();
        }
    }

    private String bindArgs(String sql, String[] args) {
        if (args == null) {
            return sql;
        }

        String printSql = sql;
        for (String arg : args) {
            printSql = printSql.replaceFirst("\\?", "'" + arg + "'");
        }
        return printSql;
    }

    /**
     * Deprecated. Will be removed in a future version of the SDK.
     * <p>
     * Returns products that have a discounted price and a valid image url.
     */
    public Product[] getDiscountedProducts() {
        Shop shop = project.getCheckedInShop();

        String id = shop != null ? shop.getId() : "";
        String query = "WHERE p.sku IN (SELECT DISTINCT sku FROM prices " +
                "WHERE discountedPrice IS NOT NULL AND pricingCategory = ifnull((SELECT pricingCategory FROM shops WHERE shops.id = '" + id + "'), '0')) " +
                "AND p.imageUrl IS NOT NULL";

        return queryDiscountedProducts(query, null);
    }

    private Product[] queryDiscountedProducts(String whereClause, String[] args) {
        Cursor cursor = productQuery(whereClause, args, false);

        if (cursor != null) {
            return allProductsAtCursor(cursor);
        } else {
            return new Product[0];
        }
    }

    private Product[] allProductsAtCursor(Cursor cursor) {
        if (cursor == null) {
            return new Product[0];
        }

        int count = cursor.getCount();
        Product[] products = new Product[count];
        int i = 0;
        while (cursor.moveToNext()) {
            products[i] = productAtCursor(cursor);
            i++;
        }
        cursor.close();
        return products;
    }

    private Product getFirstProductAndClose(Cursor cursor) {
        Product product = null;

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                product = productAtCursor(cursor);
            }

            cursor.close();

            return product;
        } else {
            return null;
        }
    }

    /**
     * Find a product via its SKU.
     *
     * @return The first product containing the given SKU, otherwise null if no product was found.
     */
    public Product findBySku(String sku) {
        if (sku == null || sku.length() == 0) {
            return null;
        }

        Cursor cursor = productQuery("WHERE p.sku = ? LIMIT 1", new String[]{
                sku
        }, false);

        return getFirstProductAndClose(cursor);
    }

    /**
     * Finds a product via its SKU over the network, if the service is available.
     * <p>
     * Searches the local database first before making any network calls.
     */
    public void findBySkuOnline(String sku, OnProductAvailableListener productAvailableListener) {
        findBySkuOnline(sku, productAvailableListener, false);
    }

    /**
     * Finds a product via its SKU over the network, if the service is available.
     * <p>
     * If onlineOnly is true, it does not search the local database first and only searches online.
     */
    public void findBySkuOnline(String sku,
                                OnProductAvailableListener productAvailableListener,
                                boolean onlineOnly) {
        if (productAvailableListener == null) {
            return;
        }

        if (onlineOnly || !isUpToDate()) {
            productApi.findBySku(sku, productAvailableListener);
        } else {
            Product local = findBySku(sku);
            if (local != null) {
                productAvailableListener.onProductAvailable(local, false);
            } else {
                productApi.findBySku(sku, productAvailableListener);
            }
        }
    }

    /**
     * Finds a product via its scannable code over the network, if the service is available.
     * <p>
     * Searches the local database first before making any network calls.
     */
    public void findByCodeOnline(ScannableCode scannableCode, OnProductAvailableListener productAvailableListener) {
        findByCodeOnline(scannableCode, productAvailableListener, false);
    }

    /**
     * Finds a product via its scannable code over the network, if the service is available.
     * <p>
     * If onlineOnly is true, it does not search the local database first and only searches online.
     */
    public void findByCodeOnline(ScannableCode scannableCode,
                                 OnProductAvailableListener productAvailableListener,
                                 boolean onlineOnly) {
        if (productAvailableListener == null) {
            return;
        }

        if (onlineOnly || !isUpToDate()) {
            productApi.findByCode(scannableCode, productAvailableListener);
        } else {
            Product local = findByCode(scannableCode);
            if (local != null) {
                productAvailableListener.onProductAvailable(local, false);
            } else {
                productApi.findByCode(scannableCode, productAvailableListener);
            }
        }
    }

    /**
     * Find a product via its SKU.
     *
     * @return The first product containing the given SKU, otherwise null if no product was found.
     */
    public Product[] findBySkus(String[] skus) {
        if (skus == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("('");
        for (String sku : skus) {
            sb.append(sku);
            sb.append("','");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(")");

        Cursor cursor = productQuery("WHERE p.sku IN " + sb.toString(), null, false);
        return allProductsAtCursor(cursor);
    }

    /**
     * Find a product via its scannable code.
     *
     * @param scannableCode A valid scannableCode code.
     * @return The first product containing the given EAN, otherwise null if no product was found.
     */
    public Product findByCode(ScannableCode scannableCode) {
        if (scannableCode == null
                || scannableCode.getLookupCode() == null
                || scannableCode.getLookupCode().length() == 0
                || scannableCode.getTemplateName() == null) {
            return null;
        }

        Cursor cursor = productQuery("JOIN scannableCodes s ON s.sku = p.sku " +
                "WHERE s.code = ? AND s.template = ? LIMIT 1", new String[]{
                scannableCode.getLookupCode(),
                scannableCode.getTemplateName()
        }, false);

        return getFirstProductAndClose(cursor);
    }

    private Product[] findBundlesOfProduct(Product product) {
        Cursor cursor = productQuery("WHERE p.bundledSku = ?", new String[]{
                product.getSku()
        }, false);

        if (cursor != null) {
            Product[] products = new Product[cursor.getCount()];

            int i = 0;
            while (cursor.moveToNext()) {
                products[i] = productAtCursor(cursor);
                i++;
            }

            cursor.close();

            return products;
        }

        return new Product[0];
    }

    /**
     * This function needs config value generateSearchIndex set to true
     * <p>
     * Returns a {@link Cursor} which can be iterated for items containing the given search
     * string at the start of a word.
     * <p>
     * Matching is normalized, so "appl" finds products containing
     * "Apple", "apple" and "Super apple", but not "Superapple".
     *
     * @param cancellationSignal Calls can be cancelled with a {@link CancellationSignal}. Can be null.
     */
    public Cursor searchByFoldedName(String searchString, CancellationSignal cancellationSignal) {
        return productQuery("JOIN searchByName ns ON ns.sku = p.sku " +
                "WHERE ns.foldedName MATCH ? " +
                "AND p.weighing != " + Product.Type.PreWeighed.getDatabaseValue() + " " +
                "AND p.isDeposit = 0 " +
                "LIMIT 100", new String[]{
                searchString + "*"
        }, true, cancellationSignal);
    }

    /**
     * Returns a {@link Cursor} which can be iterated for items containing the given scannable code.
     * <p>
     * Allows for partial matching. "978" finds products containing "978020137962".
     *
     * @param cancellationSignal Calls can be cancelled with a {@link CancellationSignal}. Can be null.
     */
    public Cursor searchByCode(String searchString, CancellationSignal cancellationSignal) {
        // constructing a common query that gets repeated two times, one for the search
        // using the "00000" prefix to match ean 8 in ean 13 codes
        // and one for all other matches

        // UNION ALL is used for two reasons:
        // 1) sorting the ean 8 matches on top without using ORDER BY for performance reasons
        // 2) avoiding the usage of OR between two GLOB's because in sqlite versions < 3.8
        // the query optimizer chooses to do a full table search instead of a scan
        String commonSql = "JOIN scannableCodes s ON s.sku = p.sku " +
                "WHERE s.code GLOB ? AND s.template = 'default' " +
                "AND p.weighing != " + Product.Type.PreWeighed.getDatabaseValue() + " " +
                "AND p.isDeposit = 0 ";

        String query = productSqlString(commonSql, true);
        query += " UNION ALL ";
        query += productSqlString(commonSql, true);
        query += " LIMIT 100";

        return rawQuery(query, new String[]{
                "00000" + searchString + "*",
                searchString + "*"
        }, cancellationSignal);
    }

    private void notifyOnDatabaseUpdated() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (OnDatabaseUpdateListener listener : onDatabaseUpdateListeners) {
                    listener.onDatabaseUpdated();
                }
            }
        });
    }

    /**
     * Adds a listener that gets called every time the database updates after calling {@link #update()}
     */
    public void addOnDatabaseUpdateListener(OnDatabaseUpdateListener onDatabaseUpdateListener) {
        if (!onDatabaseUpdateListeners.contains(onDatabaseUpdateListener)) {
            onDatabaseUpdateListeners.add(onDatabaseUpdateListener);
        }
    }

    /**
     * Removes an already added listener
     */
    public void removeOnDatabaseUpdateListener(OnDatabaseUpdateListener onDatabaseUpdateListener) {
        onDatabaseUpdateListeners.remove(onDatabaseUpdateListener);
    }

    public interface OnDatabaseUpdateListener {
        void onDatabaseUpdated();
    }

    public interface UpdateCallback {
        void success();

        void error();
    }
}




