package io.snabble.sdk;


import android.os.Handler;
import android.os.HandlerThread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

class ShoppingCartManager {
    private Project project;
    private ShoppingCart shoppingCart;
    private File file;
    private Handler backgroundHandler;

    public ShoppingCartManager(final Project project) {
        this.project = project;
        file = new File(project.getInternalStorageDirectory(), "shoppingCart.json");

        HandlerThread handlerThread = new HandlerThread("ShoppingCartManager");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        load();

        shoppingCart.addListener(new ShoppingCart.SimpleShoppingCartListener() {
            @Override
            public void onChanged(ShoppingCart list) {
                save();
            }
        });
    }

    private void load() {
        try {
            if (file.exists()) {
                String contents = IOUtils.toString(new FileInputStream(file), Charset.forName("UTF-8"));
                shoppingCart = GsonHolder.get().fromJson(contents, ShoppingCart.class);
                shoppingCart.initWithProject(project);
            } else {
                shoppingCart = new ShoppingCart(project);
            }
        } catch (Exception e) {
            //shopping cart could not be read, create a new one.
            Logger.e("Could not load shopping list from: " + file.getAbsolutePath() + ", creating a new one.");
            shoppingCart = new ShoppingCart(project);
        }
    }

    private void save() {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                String json = GsonHolder.get().toJson(shoppingCart);
                try {
                    FileUtils.forceMkdirParent(file);
                    IOUtils.write(json, new FileOutputStream(file), Charset.forName("UTF-8"));
                } catch (IOException e) {
                    //could not save shopping cart, silently ignore
                    Logger.e("Could not save shopping list for " + project.getId() + ": " + e.getMessage());
                }
            }
        });
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }
}
