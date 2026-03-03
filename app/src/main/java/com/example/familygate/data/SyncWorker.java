package com.example.familygate.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        LocalRuleStore store = new LocalRuleStore(getApplicationContext());
        String url = store.getPocketBaseUrl();
        String email = store.getPocketBaseEmail();
        String password = store.getPocketBasePassword();
        String childDeviceId = store.getChildDeviceId();

        if (url.isEmpty() || email.isEmpty() || password.isEmpty() || childDeviceId.isEmpty()) {
            return Result.failure();
        }

        PocketBaseClient client = new PocketBaseClient();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        client.signIn(url, email, password, new PocketBaseClient.ResultCallback<String>() {
            @Override
            public void onSuccess(String token) {
                client.fetchRules(url, token, childDeviceId, new PocketBaseClient.ResultCallback<List<AppRule>>() {
                    @Override
                    public void onSuccess(List<AppRule> rules) {
                        store.saveRules(rules);
                        success.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onError(String message) {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }

        return success.get() ? Result.success() : Result.retry();
    }
}
