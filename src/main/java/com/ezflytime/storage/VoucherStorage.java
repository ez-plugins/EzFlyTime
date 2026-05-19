package com.ezflytime.storage;

import java.util.Set;

public interface VoucherStorage {

    Set<String> loadConsumedVoucherIds();

    void saveConsumedVoucherIds(Set<String> consumedVoucherIds);

    default void close() {
        // Default no-op.
    }
}
