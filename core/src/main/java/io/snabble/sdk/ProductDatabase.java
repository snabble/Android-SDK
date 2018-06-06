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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

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

    public static class Config {
        String bundledAssetPath;
        long bundledRevisionId;
        int bundledSchemaVersionMajor;
        int bundledSchemaVersionMinor;
        boolean autoUpdateIfMissing;
    }

    private SQLiteDatabase db;
    private Product.Type[] productTypes = Product.Type.values();

    private long revisionId;

    private final Object dbLock = new Object();
    private String dbName;
    private String bundledAssetPath;
    private long bundledRevisionId;
    private int bundledSchemaVersionMajor;
    private int bundledSchemaVersionMinor;

    private List<OnDatabaseUpdateListener> onDatabaseUpdateListeners = new CopyOnWriteArrayList<>();
    private Date lastUpdateDate;
    private int schemaVersionMajor;
    private int schemaVersionMinor;

    private SnabbleSdk sdk;
    private Application application;
    private ProductDatabaseDownloader productDatabaseDownloader;

    private Handler handler = new Handler(Looper.getMainLooper());
    private ProductApi productApi;

    ProductDatabase(SnabbleSdk sdk,
                    String name,
                    Config config,
                    final ProductDatabaseReadyListener productDatabaseReadyListener) {
        this.sdk = sdk;
        this.application = sdk.getApplication();
        this.dbName = name;

        if(config == null){
            config = new Config();
        }

        this.bundledAssetPath = config.bundledAssetPath;
        this.bundledRevisionId = config.bundledRevisionId;
        this.bundledSchemaVersionMajor = config.bundledSchemaVersionMajor;
        this.bundledSchemaVersionMinor = config.bundledSchemaVersionMinor;

        this.productDatabaseDownloader = new ProductDatabaseDownloader(sdk, this);
        this.productApi = new ProductApi(sdk);

        if(dbName != null) {
            File file = application.getDatabasePath(dbName);

            if (!file.exists()) {
                if (bundledAssetPath != null) {
                    if (!copyDbFromAssets()) {
                        if (productDatabaseReadyListener != null) {
                            productDatabaseReadyListener.onError(Error.INTERNAL_STORAGE_FULL);
                        }
                    }
                }
            }
        } else {
            Logger.i("Product database is missing. Offline products are not available.");
        }

        if (open(true)) {
            if (productDatabaseReadyListener != null) {
                productDatabaseReadyListener.onReady(this);
            }
        } else {
            if (dbName != null && config.autoUpdateIfMissing) {
                revisionId = -1;

                update(new UpdateCallback() {
                    @Override
                    public void success() {
                        if (productDatabaseReadyListener != null) {
                            productDatabaseReadyListener.onReady(ProductDatabase.this);
                        }
                    }

                    @Override
                    public void error() {
                        if (productDatabaseReadyListener != null) {
                            productDatabaseReadyListener.onError(Error.CONNECTION_TIMEOUT);
                        }
                    }
                });
            }
        }
    }

    private ProductDatabase(SnabbleSdk sdk, String name){
        this(sdk, name, null, null);
    }

    private boolean open(boolean allowCopyFromBundle) {
        if(dbName == null){
            return true;
        }

        synchronized (dbLock) {
            File file = application.getDatabasePath(dbName);

            try {
                db = SQLiteDatabase.openDatabase(file.getAbsolutePath(),
                        null, SQLiteDatabase.OPEN_READWRITE);

                try {
                    revisionId = Long.parseLong(getMetaData(METADATA_KEY_REVISION));
                } catch (NumberFormatException e) {
                    revisionId = -1;
                }

                schemaVersionMajor = Integer.parseInt(getMetaData(METADATA_KEY_SCHEMA_VERSION_MAJOR));
                schemaVersionMinor = Integer.parseInt(getMetaData(METADATA_KEY_SCHEMA_VERSION_MINOR));

                boolean bundleHasNewerSchema = bundledSchemaVersionMajor != -1
                        && bundledSchemaVersionMinor != -1
                        && (bundledSchemaVersionMajor > schemaVersionMajor
                        || bundledSchemaVersionMinor > schemaVersionMinor);

                String project = getMetaData(METADATA_KEY_PROJECT);
                boolean isOtherProject = !sdk.getProjectId().equals(project);

                if (allowCopyFromBundle && (revisionId < bundledRevisionId
                                || bundleHasNewerSchema
                                || isOtherProject)) {
                    close();

                    if (copyDbFromAssets()) {
                        if (bundleHasNewerSchema) {
                            Logger.d("Bundled product database has newer schema (%d.%d -> %d.%d)",
                                    schemaVersionMajor, schemaVersionMinor,
                                    bundledSchemaVersionMajor, bundledSchemaVersionMinor);
                        } else if (isOtherProject) {
                            Logger.d("Bundled product database is has different projectId (%s -> %s)",
                                    project, sdk.getProjectId());
                        } else {
                            Logger.d("Bundled product database is newer (%d -> %d)",
                                    revisionId, bundledRevisionId);
                        }

                        open(false);
                    } else {
                        close();
                        return false;
                    }
                } else {
                    Logger.d("Loaded product database revision %d, schema version %d.%d",
                            revisionId, schemaVersionMajor, schemaVersionMinor);
                }

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

    private void putMetaData(String key, String value) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("key", key);
        contentValues.put("value", value);
        db.replace("metadata", null, contentValues);
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
        lastUpdateDate = new Date(timestamp);
        Logger.d("Updating last update timestamp: %s", lastUpdateDate.toString());
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
        if(dbName == null){
            return;
        }

        Logger.d("Applying delta update...");

        long fromRevisionId = getRevisionId();
        long time = SystemClock.elapsedRealtime();

        File dbFile = application.getDatabasePath(dbName);
        File tempDbFile = application.getDatabasePath("_" + dbName);

        SQLiteDatabase tempDbBUG123 = SQLiteDatabase.openOrCreateDatabase(tempDbFile, null);
        tempDbBUG123.beginTransaction();

        if (!deleteDatabase(tempDbFile)) {
            Logger.e("Could not apply delta update: Could not delete temp database");
            return;
        }

        FileUtils.copyFile(dbFile, tempDbFile);

        SQLiteDatabase tempDb = SQLiteDatabase.openOrCreateDatabase(tempDbFile, null);

        Scanner scanner = new Scanner(inputStream, "UTF-8");

        //delta update statements are splitted by ;\n\n - occurrences of ;\n\n in strings are
        //escaped replaced by the backend - splitting by just ; is not enough because of eventual
        //occurrences in database rows
        scanner.useDelimiter(";\n\n");

        try {
            tempDb.beginTransaction();
        } catch (SQLiteException e){
            Logger.e("Could not apply delta update: Could not access temp database");
            return;
        }

        while (scanner.hasNext()) {
            try {
                String line = scanner.next();

                //Scanner catches IOExceptions and stores the last thrown exception in ioException()
                //because we want to handle possible IOExceptions we throw them here again if they
                //occurred
                IOException lastException = scanner.ioException();
                if (lastException != null) {
                    Logger.e("Could not apply delta update: %s", lastException.getMessage());
                    tempDb.close();
                    deleteDatabase(tempDbFile);
                    throw lastException;
                }

                tempDb.execSQL(line);
            } catch (SQLiteException e) {
                //code 0 = "not an error" - statements like empty strings are causing that exception
                //which we want to allow
                if (!e.getMessage().contains("code 0")) {
                    Logger.e("Could not apply delta update: %s", e.getMessage());
                    tempDb.close();
                    deleteDatabase(tempDbFile);
                    return;
                }
            }
        }

        try {
            tempDb.setTransactionSuccessful();
            tempDb.endTransaction();
        } catch (SQLiteException e){
            Logger.e("Could not apply delta update: Could not finish transaction on temp database");
            return;
        }

        //delta updates are making the database grow larger and larger, so we vacuum here
        //to keep the database as small as possible
        long vacuumTime = SystemClock.elapsedRealtime();
        try {
            tempDb.execSQL("VACUUM");
        } catch (SQLiteException e) {
            Logger.e("Could not apply delta update: %s", e.getMessage());
            deleteDatabase(tempDbFile);
            return;
        }

        long vacuumTime2 = SystemClock.elapsedRealtime() - vacuumTime;
        Logger.d("VACUUM took %d ms", vacuumTime2);

        tempDb.close();

        swap(tempDbFile);

        long time2 = SystemClock.elapsedRealtime() - time;
        Logger.d("Delta update (%d -> %d) took %d ms", fromRevisionId, getRevisionId(), time2);

        updateLastUpdateTimestamp(System.currentTimeMillis());

        notifyOnDatabaseUpdated();

        return;
    }

    /**
     * Applies a full update from an input stream. The input stream should contain a full and valid
     * SQLite3 Database.
     * <p>
     * If a read error occurs, an IOException will be thrown.
     */
    synchronized void applyFullUpdate(InputStream inputStream) throws IOException {
        if(dbName == null){
            return;
        }

        Logger.d("Applying full update...");

        long time = SystemClock.elapsedRealtime();

        File tempDbFile = application.getDatabasePath("_" + dbName);

        if (!deleteDatabase(tempDbFile)) {
            Logger.e("Could not apply full update: Could not delete old database download");
            return;
        }

        tempDbFile.getParentFile().mkdirs();

        IOUtils.copy(inputStream, new FileOutputStream(tempDbFile));

        swap(tempDbFile);

        long time2 = SystemClock.elapsedRealtime() - time;
        Logger.d("Full update took %d ms", time2);

        updateLastUpdateTimestamp(System.currentTimeMillis());

        notifyOnDatabaseUpdated();
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
     * @param callback A {@link UpdateCallback} that returns success when the operation is successfully completed.
     *                 Or error() in case a network error occurred.
     */
    public void update(final UpdateCallback callback) {
        if(dbName == null){
            return;
        }

        productDatabaseDownloader.invalidate();
        productDatabaseDownloader.loadAsync(new Downloader.Callback() {
            @Override
            protected void onDataLoaded(boolean wasStillValid) {
                if (callback != null) {
                    callback.success();
                }
            }

            @Override
            protected void onError() {
                if (productDatabaseDownloader.wasSameRevision()) {
                    if (callback != null) {
                        callback.success();
                    }
                } else {
                    if (callback != null) {
                        callback.error();
                    }
                }
            }
        });
    }

    /**
     * Updates the current database in the background by requesting new data from the backend.
     * <p>
     * Updates can be either delta updates or full updates depending on how big the difference between
     * the last update was.
     * <p>
     * While updating, the database can still be queried for data, after the update completes calls to the database
     * return the updated data.
     */
    public void update() {
        update(null);
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
            if (!application.deleteDatabase(dbFile.getName())) {
                return false;
            }
        }
        return true;
    }

    private void swap(File otherDbFile) throws IOException {
        boolean ok = true;

        ProductDatabase otherDb = new ProductDatabase(sdk, otherDbFile.getName());
        try {
            otherDb.open(false);

            if (!otherDb.verify()) {
                ok = false;
            }
        } catch (Exception e) {
            ok = false;
        }

        otherDb.close();

        if (!ok) {
            Logger.e("Could not swap database: " +
                    "malformed database or unknown schema version");
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
                openOk = open(false);
            } catch (SQLiteException e) {
                openOk = false;
            }

            if (!openOk) {
                Logger.e("Could not open database after applying full update");
                Logger.d("Recovering from database in assets");

                close();
                application.deleteDatabase(dbName);
                copyDbFromAssets();
                open(false);
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

    private boolean copyDbFromAssets() {
        if (dbName == null || bundledAssetPath == null) {
            return false;
        }

        long time = SystemClock.elapsedRealtime();

        try {
            InputStream is = application.getResources().getAssets().open(bundledAssetPath);
            File outputFile = application.getDatabasePath(dbName);
            //noinspection ResultOfMethodCallIgnored
            outputFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(outputFile);
            IOUtils.copy(is, fos);
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

        builder.setSku(anyToString(cursor, 0))
                .setName(cursor.getString(1))
                .setDescription(cursor.getString(2))
                .setImageUrl(ensureNotNull(cursor.getString(3)));

        String depositSku = anyToString(cursor, 4);

        builder.setDepositProductSku(anyToString(cursor, 4))
                .setIsDeposit(cursor.getInt(5) != 0)
                .setType(productTypes[cursor.getInt(6)]);

        builder.setDepositProduct(findBySku(depositSku));

        String codes = cursor.getString(7);
        if (codes != null) {
            builder.setScannableCodes(codes.split(","));
        }

        builder.setPrice(cursor.getInt(8))
                .setDiscountedPrice(cursor.getInt(9));

        String weighedItemIds = cursor.getString(10);
        if (weighedItemIds != null) {
            builder.setWeighedItemIds(weighedItemIds.split(","));
        }

        builder.setBoost(cursor.getInt(11))
                .setSubtitle(cursor.getString(12))
                .setBasePrice(cursor.getString(13));

        if(schemaVersionMajor >= 1 && schemaVersionMinor >= 6) {
            builder.setSaleRestriction(decodeSaleRestriction(cursor.getLong(14)));
            builder.setSaleStop(cursor.getInt(15) != 0);
        }

        return builder.build();
    }

    private Product.SaleRestriction decodeSaleRestriction(long encodedValue){
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

    private Cursor productQuery(String appendSql, String[] args, boolean distinct, CancellationSignal cancellationSignal) {
        String sql = "SELECT " + (distinct ? "DISTINCT " : "") +
                "p.sku," +
                "p.name," +
                "p.description," +
                "p.imageUrl," +
                "p.depositSku," +
                "p.isDeposit," +
                "p.weighing," +
                "(SELECT group_concat(s.code) FROM scannableCodes s WHERE s.sku = p.sku)," +
                "pr.listPrice," +
                "pr.discountedPrice," +
                "(SELECT group_concat(w.weighItemId) FROM weighItemIds w WHERE w.sku = p.sku)," +
                "p.boost," +
                "p.subtitle," +
                "pr.basePrice";

                if(schemaVersionMajor >= 1 && schemaVersionMinor >= 6) {
                    sql += ",p.saleRestriction";
                    sql += ",p.saleStop";
                }

                sql += " FROM products p " +
                "JOIN prices pr ON pr.sku = p.sku " +
                appendSql;

        return rawQuery(sql, args, cancellationSignal);
    }

    public boolean isAvailableOffline() {
        return db != null;
    }

    private Cursor productQuery(String appendSql, String[] args, boolean distinct) {
        return productQuery(appendSql, args, distinct, null);
    }

    private Cursor rawQuery(String sql, String[] args, CancellationSignal cancellationSignal) {
        if (db == null) {
            return null;
        }

        long time = SystemClock.elapsedRealtime();
        Cursor cursor;

        synchronized (dbLock) {
            cursor = db.rawQuery(sql, args, cancellationSignal);
        }

        //query executes when we call the first function that needs data, not on db.rawQuery
        int count = cursor.getCount();

        long time2 = SystemClock.elapsedRealtime() - time;
        if (time2 > 16) {
            Logger.d("Query performance warning (%d ms, %d rows) for SQL: %s",
                    time2, count, bindArgs(sql, args));
        }

        return cursor;
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
     * Returns products that have and a valid image url and have set the boost flag.
     */
    public Product[] getBoostedProducts(int limit) {
        return queryDiscountedProducts("WHERE p.imageUrl IS NOT NULL" +
                        " AND p.boost > 0 ORDER BY boost DESC LIMIT ?",
                new String[]{String.valueOf(limit)});
    }

    /**
     * Returns products that have a discounted price and a valid image url.
     */
    public Product[] getDiscountedProducts() {
        return queryDiscountedProducts("WHERE pr.discountedPrice IS NOT NULL" +
                " AND p.imageUrl IS NOT NULL", null);
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
        if(cursor == null){
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

        if (onlineOnly) {
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
    public void findByCodeOnline(String code, OnProductAvailableListener productAvailableListener) {
        findByCodeOnline(code, productAvailableListener, false);
    }

    /**
     * Finds a product via its scannable code over the network, if the service is available.
     * <p>
     * If onlineOnly is true, it does not search the local database first and only searches online.
     */
    public void findByCodeOnline(String code,
                                 OnProductAvailableListener productAvailableListener,
                                 boolean onlineOnly) {
        if (productAvailableListener == null) {
            return;
        }

        if (onlineOnly) {
            productApi.findByCode(code, productAvailableListener);
        } else {
            Product local = findByCode(code);
            if (local != null) {
                productAvailableListener.onProductAvailable(local, false);
            } else {
                productApi.findByCode(code, productAvailableListener);
            }
        }
    }

    /**
     * Find a product via its SKU.
     *
     * @return The first product containing the given SKU, otherwise null if no product was found.
     */
    public Product[] findBySkus(String... skus) {
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
     * @param code A valid scannable code. For example "978020137962".
     * @return The first product containing the given EAN, otherwise null if no product was found.
     */
    public Product findByCode(String code) {
        if (code == null || code.length() == 0) {
            return null;
        }

        Cursor cursor = productQuery("JOIN scannableCodes s ON s.sku = p.sku " +
                "WHERE s.code GLOB ? LIMIT 1", new String[]{
                code
        }, false);

        return getFirstProductAndClose(cursor);
    }

    /**
     * Find a product via its weighed item id.
     *
     * @param weighedItemId A valid weighed item id.
     * @return The first product containing the given weighed item id, otherwise null if no product was found.
     */
    public Product findByWeighItemId(String weighedItemId) {
        if (weighedItemId == null || weighedItemId.length() == 0) {
            return null;
        }

        Cursor cursor = productQuery("JOIN weighItemIds w ON w.sku = p.sku " +
                "WHERE w.weighItemId = ? LIMIT 1", new String[]{
                weighedItemId
        }, false);

        return getFirstProductAndClose(cursor);
    }

    /**
     * Finds a product via its EAN over the network, if the service is available.
     * <p>
     * Searches the local database first before making any network calls.
     */
    public void findByWeighItemIdOnline(String weighItemId, OnProductAvailableListener productAvailableListener) {
        findByWeighItemIdOnline(weighItemId, productAvailableListener, false);
    }

    /**
     * Finds a product via its EAN over the network, if the service is available.
     * <p>
     * If onlineOnly is true, it does not search the local database first and only searches online.
     */
    public void findByWeighItemIdOnline(String weighItemId,
                                        OnProductAvailableListener productAvailableListener,
                                        boolean onlineOnly) {
        if (productAvailableListener == null) {
            return;
        }

        if (onlineOnly) {
            productApi.findByWeighItemId(weighItemId, productAvailableListener);
        } else {
            Product local = findByWeighItemId(weighItemId);
            if (local != null) {
                productAvailableListener.onProductAvailable(local, false);
            } else {
                productApi.findByWeighItemId(weighItemId, productAvailableListener);
            }
        }
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

        Cursor cursor = productQuery("JOIN searchByName s ON " + getSearchIndexColumn() + " = p.sku " +
                "WHERE s.foldedName MATCH ? LIMIT 1", new String[]{
                name
        }, false);

        return getFirstProductAndClose(cursor);
    }

    /**
     * Returns a {@link Cursor} which can be iterated for items containing the given search
     * string at the start of a word.
     * <p>
     * Matching is normalized, so "appl" finds products containing
     * "Apple", "apple" and "Super apple", but not "Superapple".
     *
     * @param cancellationSignal Calls can be cancelled with a {@link CancellationSignal}. Can be null.
     */
    public Cursor searchByFoldedName(String searchString, CancellationSignal cancellationSignal) {
        return productQuery("JOIN searchByName s ON " + getSearchIndexColumn() + " = p.sku " +
                "WHERE s.foldedName MATCH ? " +
                "AND p.weighing != " + Product.Type.PreWeighed.getDatabaseValue() + " " +
                "AND p.isDeposit = 0 " +
                "LIMIT 100", new String[]{
                searchString + "*"
        }, true, cancellationSignal);
    }

    private String getSearchIndexColumn() {
        // older schema version used the docid field as a primary key
        // we changed skus to be TEXT instead of INTEGER keys, so we cant use docid anymore
        // to still support older version we set the field name here
        String indexColumn;

        if(schemaVersionMajor == 1 && schemaVersionMinor <= 4){
            indexColumn = "s.docid";
        } else {
            indexColumn = "s.sku";
        }
        return indexColumn;
    }

    /**
     * Returns a {@link Cursor} which can be iterated for items containing the given scannable code.
     * <p>
     * Allows for partial matching. "978" finds products containing "978020137962".
     *
     * @param cancellationSignal Calls can be cancelled with a {@link CancellationSignal}. Can be null.
     */
    public Cursor searchByCode(String searchString, CancellationSignal cancellationSignal) {
        return productQuery("JOIN scannableCodes s ON s.sku = p.sku " +
                "WHERE s.code GLOB ? " +
                "AND p.weighing != 1 " +
                "AND p.isDeposit = 0 " +
                "LIMIT 100", new String[]{
                searchString + "*"
        }, true, cancellationSignal);
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

    enum Error {
        INTERNAL_STORAGE_FULL,
        CONNECTION_TIMEOUT,
        DATABASE_MISSING,
    }

    interface ProductDatabaseReadyListener {
        void onReady(ProductDatabase productDatabase);

        void onError(Error error);
    }

    public interface OnDatabaseUpdateListener {
        void onDatabaseUpdated();
    }

    public interface UpdateCallback {
        void success();

        void error();
    }
}




