package com.arttvad.worktime;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

public final class DocumentRoundTripProvider extends ContentProvider {
    public static final String AUTHORITY = "com.arttvad.worktime.test.documents";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        String extension = "";
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < name.length()) {
                extension = name.substring(dot + 1);
            }
        }
        switch (extension) {
            case "csv":
                return "text/csv";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf":
                return "application/pdf";
            case "wtbk":
            default:
                return "application/octet-stream";
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = fileFor(uri);
        int flags;
        if (mode.startsWith("w")) {
            flags = ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE
                    | ParcelFileDescriptor.MODE_WRITE_ONLY;
        } else if (mode.startsWith("a")) {
            flags = ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND
                    | ParcelFileDescriptor.MODE_WRITE_ONLY;
        } else if (mode.startsWith("r")) {
            flags = ParcelFileDescriptor.MODE_READ_ONLY;
        } else {
            throw new FileNotFoundException("Unsupported mode: " + mode);
        }
        return ParcelFileDescriptor.open(file, flags);
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return fileFor(uri).delete() ? 1 : 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private File fileFor(Uri uri) {
        String rawName = uri.getLastPathSegment();
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing document name");
        }
        String name = rawName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (getContext() == null) {
            throw new IllegalStateException("Provider context unavailable");
        }
        File root = new File(getContext().getCacheDir(), "document-roundtrip");
        if (!root.exists() && !root.mkdirs()) {
            throw new IllegalStateException("Cannot create document cache directory: " + root);
        }
        return new File(root, name);
    }

    public static Uri uri(String name) {
        return Uri.parse("content://" + AUTHORITY + "/" + name);
    }
}
