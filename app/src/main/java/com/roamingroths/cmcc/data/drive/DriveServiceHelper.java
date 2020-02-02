package com.roamingroths.cmcc.data.drive;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.utils.GoogleAuthHelper;

import java.util.Collections;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class DriveServiceHelper {

  private final Drive mDrive;

  public DriveServiceHelper(Drive mDrive) {
    this.mDrive = mDrive;
  }

  public static DriveServiceHelper forAccount(GoogleSignInAccount account, Context context) {
    // Use the authenticated account to sign in to the Drive service.
    GoogleAccountCredential credential =
        GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE));
    credential.setSelectedAccount(account.getAccount());
    return new DriveServiceHelper(new Drive.Builder(
        AndroidHttp.newCompatibleTransport(),
        new GsonFactory(),
        credential)
        .setApplicationName("Drive API Migration")
        .build());
  }

  public static Maybe<DriveServiceHelper> init(Context context) {
    return GoogleAuthHelper.googleAccount(context).map(account -> forAccount(account, context));
  }

  private Maybe<File> getFolder(String folderName) {
    return Single.<FileList>create(e -> e.onSuccess(mDrive.files().list()
        .setQ(String.format("name = '%s' and mimeType = 'application/vnd.google-apps.folder'", folderName))
        .setSpaces("drive")
        .execute()))
        .flatMapMaybe(fileList -> {
          if (fileList.getFiles().isEmpty()) {
            return Maybe.empty();
          }
          if (fileList.getFiles().size() > 1) {
            return Maybe.error(
                new IllegalStateException(String.format("Duplicate folders found for %s", folderName)));
          }
          return Maybe.just(fileList.getFiles().get(0));
        }).subscribeOn(Schedulers.io());
  }

  public Single<File> getOrCreateFolder(String folderName) {
    return getFolder(folderName).switchIfEmpty(Single.create(e -> {
      Timber.d("Creating 'My Charts' folder in Drive");

      File folderMetadata = new File();
      folderMetadata.setName(folderName);
      folderMetadata.setMimeType("application/vnd.google-apps.folder");

      e.onSuccess(mDrive.files().create(folderMetadata)
          .setFields("id")
          .execute());
    })).subscribeOn(Schedulers.io());
  }

  public Observable<File> getFilesInFolder(@NonNull File folder) {
    return query(String.format("'%s' in parents", folder.getId()))
        .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
        .subscribeOn(Schedulers.io());
  }

  public Completable deleteFileFromFolder(@NonNull File folder, String filename) {
    return query(String.format("name = '%s' and '%s' in parents", filename, folder.getId()))
        .flatMapCompletable(file -> Completable.fromAction(() -> {
          mDrive.files().delete(file.getId()).execute();
        }).doOnComplete(() -> Timber.d("Deleted file")))
        .doOnSubscribe(d -> Timber.d("Looking for %s in %s", filename, folder.getName()))
        ;
  }

  public Single<File> addFileToFolder(@NonNull File folder, File fileMetadata, FileContent fileContent) {
    Timber.d("Adding file to %s", folder.getName());
    fileMetadata.setParents(ImmutableList.of(folder.getId()));
    return Single.<File>create(e -> e.onSuccess(mDrive
        .files()
        .create(fileMetadata, fileContent)
        .execute()))
        .subscribeOn(Schedulers.io());
  }

  public Completable clearFolder(@NonNull File folder) {
    Timber.d("Clearing contents of %s", folder.getName());
    return getFilesInFolder(folder)
        .flatMapCompletable(file -> Completable.create(e -> {
          mDrive.files().delete(file.getId()).execute();
          e.onComplete();
        }));
  }

  private Observable<File> query(@NonNull String query) {
    Timber.v("Query: %s", query);
    return Observable.create(e -> {
      String pageToken = null;
      do {
        FileList result = mDrive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("nextPageToken, files(id, name)")
            .setPageToken(pageToken)
            .execute();
        for (File file : result.getFiles()) {
          e.onNext(file);
        }
        pageToken = result.getNextPageToken();
      } while (pageToken != null);
      e.onComplete();
    });
  }
}
