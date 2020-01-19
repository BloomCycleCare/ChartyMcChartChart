package com.roamingroths.cmcc.data.repos;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.collect.ImmutableList;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ChartRepo {

  private final Drive googleDriveService;

  public ChartRepo(Drive googleDriveService) {
    this.googleDriveService = googleDriveService;
  }

  private Maybe<File> getFolder(String folderName) {
    return Single.<FileList>create(e -> e.onSuccess(googleDriveService.files().list()
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

      e.onSuccess(googleDriveService.files().create(folderMetadata)
          .setFields("id")
          .execute());
    })).subscribeOn(Schedulers.io());
  }

  public Observable<File> getFilesInFolder(@NonNull File folder) {
    return query(String.format("'%s' in parents", folder.getId()))
        .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
        .subscribeOn(Schedulers.io());
  }

  public Single<File> addFileToFolder(@NonNull File folder, File fileMetadata, FileContent fileContent) {
    Timber.d("Adding file to %s", folder.getName());
    fileMetadata.setParents(ImmutableList.of(folder.getId()));
    return Single.<File>create(e -> e.onSuccess(googleDriveService
        .files()
        .create(fileMetadata, fileContent)
        .execute()))
        .subscribeOn(Schedulers.io());
  }

  public Completable clearFolder(@NonNull File folder) {
    Timber.d("Clearing contents of %s", folder.getName());
    return getFilesInFolder(folder)
        .flatMapCompletable(file -> Completable.create(e -> {
          googleDriveService.files().delete(file.getId()).execute();
          e.onComplete();
        }));
  }

  private Observable<File> query(@NonNull String query) {
    return Observable.create(e -> {
      String pageToken = null;
      do {
        FileList result = googleDriveService.files().list()
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
